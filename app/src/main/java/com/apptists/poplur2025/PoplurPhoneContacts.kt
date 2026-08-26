package com.apptists.poplur2025

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The Poplur phone's address book. Port of the iOS `PoplurPhoneContacts`.
 *
 * The phone had a hardcoded Contacts menu — "Poplur Phone", "Poplur Support",
 * and "Add contact — coming soon". This assembles the real one from the four
 * sources the phone is meant to draw on:
 *
 *   1. FAVOURITES — people you chose to keep, so you can call them later.
 *   2. DMs — anyone you already have a conversation with.
 *   3. INTERCEPTS — people you and your radius have actually crossed.
 *   4. REACH — people your radius reaches RIGHT NOW.
 *
 * "Intercept" is read from `engagements`, because that is the only DURABLE
 * record that a crossing happened. Both directions count: people who engaged
 * your posts, and people whose posts you engaged.
 *
 * Reach is the live half. With history alone, two people who have never
 * interacted can never find each other, so a brand new account has an empty
 * phone and no way to fill it. In an app whose whole premise is that radius is
 * what connects people, the one thing the address book must answer is "who can
 * I call from here" — so reach is a source, marked as live, and never confused
 * with the durable tiers, which still sort above it.
 */
object PoplurPhoneContacts {

    private const val TAG = "PoplurPhone"

    private val db = FirebaseFirestore.getInstance()
    private val uid: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    /** One entry per person, however many ways they qualified. */
    private data class Contact(
        val id: String,
        var name: String,
        var favourite: Boolean = false,
        var hasDM: Boolean = false,
        var intercepted: Boolean = false,
        /** Inside your radius as of this read. Live, not durable. */
        var inReach: Boolean = false,
        /** Great-circle km from you, when reach put them here. */
        var distanceKm: Double? = null,
        /** Most recent moment this person showed up, for ordering. */
        var lastSeen: Long = Long.MIN_VALUE
    )

    /** Everything the phone needs, newest-first within each tier. */
    suspend fun contacts(
        radiusKm: Double? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): JSONArray {
        val me = uid ?: return JSONArray()
        val byId = linkedMapOf<String, Contact>()

        fun merge(id: String, name: String?, whenMillis: Long?, apply: (Contact) -> Unit) {
            if (id.isEmpty() || id == me) return
            val entry = byId.getOrPut(id) { Contact(id = id, name = name ?: id.take(8)) }
            // A real handle always beats the uid-prefix placeholder.
            if (!name.isNullOrEmpty() && (entry.name.startsWith(id.take(8)) || entry.name.isEmpty())) {
                entry.name = name
            }
            if (whenMillis != null && whenMillis > entry.lastSeen) entry.lastSeen = whenMillis
            apply(entry)
        }

        // 1. Favourites.
        runCatching {
            db.collection("users").document(me).collection("phoneFavourites").get().await()
        }.getOrNull()?.documents?.forEach { document ->
            merge(
                document.id,
                document.getString("name"),
                document.getTimestamp("addedAt")?.toDate()?.time
            ) { it.favourite = true }
        }

        // 2. Anyone you have a thread with.
        runCatching {
            db.collection("dmThreads")
                .whereArrayContains("participants", me)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get().await()
        }.getOrNull()?.documents?.forEach { document ->
            @Suppress("UNCHECKED_CAST")
            val participants = document.get("participants") as? List<String> ?: emptyList()
            val other = participants.firstOrNull { it != me } ?: ""
            @Suppress("UNCHECKED_CAST")
            val names = document.get("participantNames") as? Map<String, String>
            merge(
                other,
                names?.get(other),
                document.getTimestamp("updatedAt")?.toDate()?.time
            ) { it.hasDM = true }
        }

        // 3a. People who engaged YOUR posts. These carry a denormalised handle,
        //     so this direction names people without a profile read each.
        runCatching {
            db.collectionGroup("engagements")
                .whereEqualTo("postOwnerUid", me)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get().await()
        }.getOrNull()?.documents?.forEach { document ->
            merge(
                document.getString("actorUid").orEmpty(),
                document.getString("actorHandle"),
                document.getTimestamp("createdAt")?.toDate()?.time
            ) { it.intercepted = true }
        }

        // 3b. People whose posts YOU engaged. No handle on this side — the
        //     denormalised one belongs to the actor, which here is you.
        runCatching {
            db.collectionGroup("engagements")
                .whereEqualTo("actorUid", me)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .get().await()
        }.getOrNull()?.documents?.forEach { document ->
            merge(
                document.getString("postOwnerUid").orEmpty(),
                null,
                document.getTimestamp("createdAt")?.toDate()?.time
            ) { it.intercepted = true }
        }

        // 4. REACH — whoever has left a post on ground you can stand and touch
        // from here. Bounded by a lat/lng BOX first because Firestore cannot do
        // a radius, then filtered to the true circle: a box query alone hands
        // you the corners, which are up to 41% further out than the radius you
        // actually have.
        if (radiusKm != null && radiusKm > 0 && latitude != null && longitude != null) {
            val latDelta = radiusKm / 111.32
            // Longitude degrees shrink with latitude; at 60° a degree is half
            // the distance it is at the equator. Clamped so the cosine cannot
            // approach zero near the poles and open the box to the whole world.
            val lngDelta = radiusKm / max(1.0, 111.32 * cos(latitude * Math.PI / 180))
            // Only the modern, explicitly-public origin contract participates
            // in live discovery. The old lat/lng pair had no trustworthy
            // privacy marker, so using it as a fallback could expose a private
            // account through a pre-migration post.
            runCatching {
                db.collection("posts")
                    .whereGreaterThan("latitude", latitude - latDelta)
                    .whereLessThan("latitude", latitude + latDelta)
                    .limit(200)
                    .get().await()
            }.getOrNull()?.documents?.forEach { document ->
                val data = document.data ?: return@forEach
                if (!hasDiscoverableOrigin(data)) return@forEach
                val postLat = document.getDouble("latitude") ?: return@forEach
                val postLng = document.getDouble("longitude") ?: return@forEach
                if (abs(postLng - longitude) > lngDelta) return@forEach
                val km = haversineKm(latitude, longitude, postLat, postLng)
                if (km > radiusKm) return@forEach
                val owner = document.getString("userID")
                    ?: document.getString("userId")
                    ?: ""
                merge(
                    owner,
                    document.getString("username"),
                    document.getTimestamp("createdAt")?.toDate()?.time
                ) { entry ->
                    entry.inReach = true
                    // Nearest post wins: that is how far away they are.
                    if (entry.distanceKm == null || km < entry.distanceKm!!) entry.distanceKm = km
                }
            }
        }

        fillMissingNames(byId)

        // Favourites first — that is what favouriting is FOR. Then the people
        // you have actually met, newest first. Reach-only contacts come last and
        // are ordered by DISTANCE, because for someone you have never met the
        // only thing you know is how far away they are; among those, nearest is
        // the most callable.
        val sorted = byId.values.sortedWith(Comparator { a, b ->
            if (a.favourite != b.favourite) return@Comparator if (a.favourite) -1 else 1
            val aKnown = a.hasDM || a.intercepted
            val bKnown = b.hasDM || b.intercepted
            if (aKnown != bKnown) return@Comparator if (aKnown) -1 else 1
            if (!aKnown && !bKnown) {
                val aKm = a.distanceKm ?: Double.MAX_VALUE
                val bKm = b.distanceKm ?: Double.MAX_VALUE
                return@Comparator aKm.compareTo(bKm)
            }
            b.lastSeen.compareTo(a.lastSeen)
        })

        val out = JSONArray()
        sorted.forEach { contact ->
            out.put(JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("favourite", contact.favourite)
                put("hasDM", contact.hasDM)
                put("inReach", contact.inReach)
                contact.distanceKm?.let { put("distanceKm", it) }
                // What to say under the name, most specific reason first. A
                // reach-only contact gets the distance, because that IS the
                // reason they are callable and it is the thing that will stop
                // being true when either of you moves.
                put(
                    "detail",
                    when {
                        contact.favourite -> "favourite"
                        contact.hasDM -> "you have a chat"
                        contact.intercepted -> "crossed your radius"
                        contact.inReach -> String.format(
                            "%.1f km away · in your radius",
                            contact.distanceKm ?: 0.0
                        )
                        else -> "on Poplur"
                    }
                )
            })
        }
        return out
    }

    /**
     * Great-circle km. The radius is a circle and the box query is not, so this
     * is what turns one into the other.
     */
    private fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val radius = 6371.0
        val dLat = (lat2 - lat1) * Math.PI / 180
        val dLng = (lng2 - lng1) * Math.PI / 180
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1 * Math.PI / 180) * cos(lat2 * Math.PI / 180) * sin(dLng / 2) * sin(dLng / 2)
        return radius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    /**
     * Reach fails closed: only the current explicitly-public origin contract
     * may make an account discoverable. Missing fields are fine for older
     * public posts, but a present malformed privacy marker is not permission.
     */
    private fun hasDiscoverableOrigin(data: Map<String, Any>): Boolean {
        if (data["locationVisibility"] != "approximate") return false
        for (key in listOf(
            "privateAccount",
            "ghostMode",
            "originHiddenByPrivateAccount",
            "originHiddenByGhostMode"
        )) {
            val value = data[key] ?: continue
            if (value !is Boolean || value) return false
        }
        return true
    }

    /**
     * One batched pass for anyone still showing a uid-prefix placeholder,
     * rather than a read per contact inside the merge loop.
     */
    private suspend fun fillMissingNames(byId: MutableMap<String, Contact>) {
        val needing = byId.values.filter { it.name == it.id.take(8) }.map { it.id }
        if (needing.isEmpty()) return
        needing.take(30).forEach { id ->
            val document = runCatching {
                db.collection("users").document(id).get().await()
            }.getOrNull() ?: return@forEach
            val name = document.getString("username")?.takeIf { it.isNotBlank() }
                ?: document.getString("displayName")?.takeIf { it.isNotBlank() }
            if (name != null) byId[id]?.name = name
        }
    }

    /**
     * Favourite / un-favourite. The doc id IS the other uid, so this is
     * idempotent and un-favouriting needs no query.
     */
    suspend fun setFavourite(otherUid: String, on: Boolean, name: String?): Boolean {
        val me = uid ?: return false
        if (otherUid.isEmpty() || otherUid == me) return false
        val ref = db.collection("users").document(me)
            .collection("phoneFavourites").document(otherUid)
        return try {
            if (on) {
                ref.set(
                    mapOf(
                        "name" to (name ?: ""),
                        "addedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            } else {
                ref.delete().await()
            }
            true
        } catch (error: Exception) {
            Log.w(TAG, "favourite write failed", error)
            false
        }
    }
}

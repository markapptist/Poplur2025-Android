package com.apptists.poplur2025

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Complete, screen-owned author reads, using the signed-in native session. */
internal class AuthoredPostsBridge(private val emit: (String) -> Unit) {
    private data class Entry(val generation: String, val registration: ListenerRegistration)
    private val subscriptions = mutableMapOf<String, Entry>()

    fun stopAll() {
        subscriptions.values.forEach { it.registration.remove() }
        subscriptions.clear()
    }

    /** Call on the UI thread, as with stopAll and Firestore's callbacks. */
    fun handle(body: JSONObject): Boolean {
        val type = body.optString("type")
        if (type != "watchAuthoredPosts" && type != "stopAuthoredPosts") return false
        val id = body.optString("subscriptionId")
        if (id.isBlank() || id.length > 100) return true
        subscriptions.remove(id)?.registration?.remove()
        if (type == "stopAuthoredPosts") return true
        val uid = body.optString("userId")
        val viewer = FirebaseAuth.getInstance().currentUser?.uid ?: return true
        if (uid.isBlank() || uid.length > 128 || subscriptions.size >= 4) return true
        val generation = UUID.randomUUID().toString()
        val registration = FirebaseFirestore.getInstance().collection("posts")
            .whereEqualTo("userID", uid).addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (subscriptions[id]?.generation != generation || FirebaseAuth.getInstance().currentUser?.uid != viewer) return@addSnapshotListener
                if (snapshot?.metadata?.isFromCache == true) return@addSnapshotListener
                val payload = JSONObject().put("subscriptionId", id).put("uid", uid)
                if (error != null || snapshot == null) payload.put("error", "Could not load this profile's posts.")
                else payload.put("documents", JSONArray(snapshot.documents.mapNotNull { document ->
                    publicRow(document.data.orEmpty())?.let { JSONObject().put("id", document.id).put("row", it) }
                }))
                emit("window.dispatchEvent(new CustomEvent('poplur-authored-posts',{detail:$payload}));true;")
            }
        subscriptions[id] = Entry(generation, registration)
        return true
    }

    companion object {
        private val fields = listOf("userID", "userId", "ownerId", "audience", "visibilityStatus", "status", "deleted", "isDeleted",
            "radiusMeters", "radius", "organicRadiusMeters", "seedRadiusMeters", "playbackURL", "mediaURL", "mediaType",
            "posterURL", "photoViewSeconds", "username", "handle", "displayName", "caption", "channelId", "profilePictureURL",
            "avatarURL", "avatarUrl", "photoURL", "viewCount", "likeCount", "upvoteCount", "downvoteCount", "dislikeCount",
            "commentCount", "shareCount", "syntheticJourney", "syntheticBeta", "syntheticCohort")
        internal fun publicRow(data: Map<String, Any>): JSONObject? {
            fun enabled(value: Any?) = value == true || value == 1 || value?.toString()?.trim()?.equals("true", ignoreCase = true) == true
            val audience = (data["audience"] as? String).orEmpty().trim().lowercase()
            val status = ((data["visibilityStatus"] ?: data["status"]) as? String).orEmpty().trim().lowercase()
            if ((audience.isNotEmpty() && audience != "public") ||
                (status.isNotEmpty() && status != "published" && status != "live") || enabled(data["deleted"]) || enabled(data["isDeleted"])) return null
            val row = JSONObject()
            for (key in fields) when (val value = data[key]) {
                is String, is Boolean -> row.put(key, value)
                is Number -> if (value.toDouble().isFinite()) row.put(key, value)
            }
            val created = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: data["createdAtMillis"]
            if (created is Number && created.toDouble().isFinite()) row.put("createdAtMillis", created)
            val publicOrigin = data["locationVisibility"] == "approximate" && !enabled(data["privateAccount"]) && !enabled(data["ghostMode"])
            row.put("locationVisibility", if (publicOrigin) "approximate" else "hidden")
            if (publicOrigin) for (key in listOf("latitude", "longitude", "originLatitude", "originLongitude")) {
                val value = data[key]
                if (value is Number && value.toDouble().isFinite()) row.put(key, value)
            }
            return row
        }
    }
}

package com.apptists.poplur2025

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import org.json.JSONObject

/**
 * Poplur system notifications. Parity with the iOS behaviour where the clay
 * app icon appears on lock-screen / shade notifications:
 *  - small (status-bar) icon: the monochrome clay-P silhouette (system-tinted),
 *  - large icon: the full-colour clay app icon,
 *  - accent colour: clay pink, tinting the small icon + app-name row.
 *
 * Android's small icon must be a flat silhouette (alpha only), so the colourful
 * clay tile can only ride along as the large icon — that's the platform rule,
 * not a Poplur choice.
 */
object PoplurNotifications {
    const val CHANNEL_ID = "poplur_alerts"
    private const val CHANNEL_NAME = "Poplur Alerts"
    private const val CHANNEL_DESC = "Posts, trends, and activity in your radius"

    /** Clay pink — matches the app-icon border and the web HUD accents. */
    private val ACCENT = 0xFFF63B9A.toInt()

    fun ensureChannel(context: Context) {
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            mgr.createNotificationChannel(channel)
        }
    }

    /**
     * Posts a Poplur-branded notification. No-ops (rather than crashing) when the
     * runtime POST_NOTIFICATIONS grant is missing on Android 13+.
     */
    @JvmOverloads
    fun post(
        context: Context,
        id: Int,
        title: String,
        body: String,
        silent: Boolean = false,
        destinationThreadId: String? = null,
        destinationPostId: String? = null,
    ) {
        ensureChannel(context)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_poplur)
            .setLargeIcon(appIconBitmap(context))
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSilent(silent)
            .setPriority(if (silent) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
        if (!destinationThreadId.isNullOrBlank()) {
            val destination = Intent(context, PostLoginWebActivity::class.java).apply {
                action = "com.apptists.poplur2025.OPEN_DM"
                putExtra("threadId", destinationThreadId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    destinationThreadId.hashCode(),
                    destination,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        } else if (!destinationPostId.isNullOrBlank()) {
            // An engagement/radius notification opens ON its pin. Without a
            // content intent a tap did nothing at all.
            val destination = Intent(context, PostLoginWebActivity::class.java).apply {
                action = "com.apptists.poplur2025.OPEN_POST"
                putExtra("postId", destinationPostId)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            builder.setContentIntent(
                PendingIntent.getActivity(
                    context,
                    destinationPostId.hashCode(),
                    destination,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            )
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    /**
     * The clay-P app icon (`R.mipmap.ic_launcher`) rendered to a bitmap for the
     * notification large icon. `ic_launcher` is an adaptive icon (anydpi-v26 XML),
     * so BitmapFactory.decodeResource can't read it — we resolve the drawable and
     * paint it onto a bitmap, which correctly composites the adaptive layers.
     * (Note: the `AppIconTheme` clay→ic_launcher_sparkle_pink mapping is stale —
     * sparkle_pink is the OLD four-petal logo, not the clay art — so we point at
     * ic_launcher directly here.)
     */
    private fun appIconBitmap(context: Context): Bitmap? {
        val drawable = ResourcesCompat.getDrawable(
            context.resources, R.mipmap.ic_launcher, context.theme
        ) ?: return null
        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }
        val size = 192
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        return bmp
    }

    // ── Tier system (parity with iOS PoplurNotificationCenter) ───────────
    //
    // The web bundle logs every engagement row and forwards it over the
    // bridge as {type:"notification", id, title, detail, kind}. iOS routes
    // that through three tiers; this process used to ignore the message
    // outright, so an Android install got no push for anything the web
    // logged. Same tiers, same preference defaults, same cooldown:
    //
    //   immediate — DMs, mentions, someone entering your radius
    //   batched   — likes / comments / views, summarised every 15 minutes
    //   digest    — radius and trend rollups (iOS schedules these from its
    //               own daily Firestore pass; this process has no such pass,
    //               so a digest-kind event only lands as an in-app row)

    enum class Tier { IMMEDIATE, BATCHED, DIGEST }

    enum class Kind(val raw: String) {
        ENGAGEMENT("engagement"),
        DM("dm"),
        MENTION("mention"),
        RADIUS_ENTRY("radiusEntry"),
        RADIUS_GROWTH("radiusGrowth"),
        RADIUS_DIP("radiusDip"),
        AREA_TAKEOVER("areaTakeover"),
        DAILY_INSIGHT("dailyInsight")
    }

    /** Same keys and defaults as the iOS UserDefaults preferences. */
    private const val PREFS = "poplur.notifications"
    private const val PREF_MESSAGES = "poplur.notifications.pref.messages"
    private const val PREF_ENGAGEMENT = "poplur.notifications.pref.engagement"
    private const val PREF_RADIUS = "poplur.notifications.pref.radius"
    private const val PREF_SOUNDS = "poplur.notifications.pref.sounds"
    private const val PREF_LAST_PUSH_AT = "poplur.notifications.lastPushAt"
    private const val PREF_SEEN_IDS = "poplur.notifications.seenIds"

    private const val BATCH_FLUSH_MS = 15L * 60L * 1000L   // 15 minutes
    private const val PUSH_COOLDOWN_MS = 5L * 60L * 1000L  // 5 minutes
    private const val HIGHLIGHTS_MIN_BATCH = 5
    private const val SEEN_ID_CAP = 200

    const val ENGAGEMENT_EVERY = "every"
    const val ENGAGEMENT_HIGHLIGHTS = "highlights"
    const val ENGAGEMENT_OFF = "off"
    const val RADIUS_DAILY = "daily"
    const val RADIUS_OFF = "off"

    private data class BatchItem(val webKind: String, val user: String, val detail: String)

    private val pendingBatch = mutableListOf<BatchItem>()
    private val handler = Handler(Looper.getMainLooper())
    private var flushArmed = false

    /**
     * Whether a Poplur surface is on screen. iOS reads
     * UIApplication.applicationState; there is no equivalent free read here,
     * so PostLoginWebActivity reports it from onResume/onPause. A push on top
     * of a screen the user is already looking at is redundant noise — the web
     * layer's own list already has the row.
     */
    @Volatile
    var appInForeground: Boolean = false

    /**
     * The in-app banner sink. PostLoginWebActivity installs it while Home is
     * resumed; a push arriving then is handed to the web layer, which draws its
     * own tappable banner instead of the system drawing one over the app.
     * Returns true when the banner was drawn in-app.
     */
    @Volatile
    var foregroundBannerSink: ((JSONObject) -> Boolean)? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun messagesPushEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PREF_MESSAGES, true)

    fun setMessagesPushEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(PREF_MESSAGES, value).apply()
    }

    fun engagementPushPref(context: Context): String =
        prefs(context).getString(PREF_ENGAGEMENT, ENGAGEMENT_EVERY) ?: ENGAGEMENT_EVERY

    fun setEngagementPushPref(context: Context, value: String) {
        prefs(context).edit().putString(PREF_ENGAGEMENT, value).apply()
    }

    fun radiusPushPref(context: Context): String =
        prefs(context).getString(PREF_RADIUS, RADIUS_DAILY) ?: RADIUS_DAILY

    fun setRadiusPushPref(context: Context, value: String) {
        prefs(context).edit().putString(PREF_RADIUS, value).apply()
    }

    fun soundsEnabled(context: Context): Boolean =
        prefs(context).getBoolean(PREF_SOUNDS, true)

    fun setSoundsEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(PREF_SOUNDS, value).apply()
    }

    /**
     * Routes one web-originated notification. Called on the main thread.
     */
    fun scheduleFromWeb(context: Context, id: String, title: String, detail: String, kind: String?) {
        if (id.isEmpty() || title.isEmpty()) return
        // The web re-sends its log on reconnect. Same id is the same event, and
        // a resend must not stack a second one on the lock screen.
        if (!markSeen(context, id)) return

        val resolved = resolveKind(kind)
        when (classifyTier(resolved)) {
            Tier.IMMEDIATE ->
                if (shouldPushKind(context, resolved)) {
                    push(context, id, title, detail, resolved)
                }
            Tier.BATCHED ->
                if (engagementPushPref(context) != ENGAGEMENT_OFF) {
                    pendingBatch.add(BatchItem(kind ?: "like", title, detail))
                    armFlush(context)
                }
            Tier.DIGEST -> Unit
        }
    }

    private fun resolveKind(raw: String?): Kind = when (raw) {
        "like", "view", "comment" -> Kind.ENGAGEMENT
        "dm", "message" -> Kind.DM
        "mention" -> Kind.MENTION
        "radiusEntry" -> Kind.RADIUS_ENTRY
        "radiusGrowth" -> Kind.RADIUS_GROWTH
        "radiusDip" -> Kind.RADIUS_DIP
        "areaTakeover" -> Kind.AREA_TAKEOVER
        "dailyInsight" -> Kind.DAILY_INSIGHT
        else -> Kind.ENGAGEMENT
    }

    private fun classifyTier(kind: Kind): Tier = when (kind) {
        Kind.DM, Kind.MENTION, Kind.RADIUS_ENTRY -> Tier.IMMEDIATE
        Kind.ENGAGEMENT -> Tier.BATCHED
        Kind.RADIUS_GROWTH, Kind.RADIUS_DIP, Kind.AREA_TAKEOVER, Kind.DAILY_INSIGHT -> Tier.DIGEST
    }

    private fun shouldPushKind(context: Context, kind: Kind): Boolean = when (kind) {
        Kind.DM, Kind.MENTION -> messagesPushEnabled(context)
        Kind.RADIUS_ENTRY -> true
        Kind.ENGAGEMENT -> engagementPushPref(context) != ENGAGEMENT_OFF
        Kind.RADIUS_GROWTH, Kind.RADIUS_DIP, Kind.AREA_TAKEOVER, Kind.DAILY_INSIGHT ->
            radiusPushPref(context) != RADIUS_OFF
    }

    private fun push(context: Context, id: String, title: String, body: String, kind: Kind) {
        if (appInForeground) return
        post(
            context = context,
            id = id.hashCode(),
            title = title,
            body = body,
            silent = !soundsEnabled(context)
        )
        prefs(context).edit().putLong(PREF_LAST_PUSH_AT, System.currentTimeMillis()).apply()
    }

    // ── Batch flush ─────────────────────────────────────────────────────
    //
    // iOS keeps a repeating 15-minute Timer alive for the life of the
    // process. This one is armed only while something is actually waiting,
    // because an always-on wheel is exactly the background wake budget this
    // app has been burned by before.

    private fun armFlush(context: Context) {
        if (flushArmed) return
        flushArmed = true
        val app = context.applicationContext
        handler.postDelayed({
            flushArmed = false
            flushPendingBatch(app)
            if (pendingBatch.isNotEmpty()) armFlush(app)
        }, BATCH_FLUSH_MS)
    }

    private fun flushPendingBatch(context: Context) {
        if (pendingBatch.isEmpty()) return
        if (engagementPushPref(context) == ENGAGEMENT_OFF) {
            pendingBatch.clear()
            return
        }
        // Keep the batch — flush next time if the user has left.
        if (appInForeground) return

        val lastPushAt = prefs(context).getLong(PREF_LAST_PUSH_AT, 0L)
        if (lastPushAt > 0L && System.currentTimeMillis() - lastPushAt < PUSH_COOLDOWN_MS) return

        val batch = pendingBatch.toList()
        pendingBatch.clear()

        if (engagementPushPref(context) == ENGAGEMENT_HIGHLIGHTS &&
            batch.size < HIGHLIGHTS_MIN_BATCH
        ) {
            return
        }

        val (title, body) = composeBatchSummary(batch)
        push(context, "batch-${System.currentTimeMillis()}", title, body, Kind.ENGAGEMENT)
    }

    private fun composeBatchSummary(batch: List<BatchItem>): Pair<String, String> {
        val likes = batch.count { it.webKind == "like" }
        val comments = batch.count { it.webKind == "comment" }
        val views = batch.count { it.webKind == "view" }

        val parts = mutableListOf<String>()
        if (likes > 0) parts.add("$likes like${if (likes == 1) "" else "s"}")
        if (comments > 0) parts.add("$comments comment${if (comments == 1) "" else "s"}")
        if (views > 0) parts.add("$views view${if (views == 1) "" else "s"}")

        val title = if (batch.size == 1) {
            batch.first().user
        } else {
            "${batch.size} people interacted with your posts"
        }
        val body = parts.joinToString(", ").ifEmpty { "New activity on your posts" }
        return title to body
    }

    /**
     * Records an id as handled. Returns false when it was already seen.
     * iOS gets this for free from its persisted in-app list; the list is a web
     * surface here, so the dedupe ring has to live on this side.
     */
    private fun markSeen(context: Context, id: String): Boolean {
        val store = prefs(context)
        val raw = store.getString(PREF_SEEN_IDS, "") ?: ""
        val seen = if (raw.isEmpty()) mutableListOf() else raw.split("\n").toMutableList()
        if (seen.contains(id)) return false
        seen.add(id)
        while (seen.size > SEEN_ID_CAP) seen.removeAt(0)
        store.edit().putString(PREF_SEEN_IDS, seen.joinToString("\n")).apply()
        return true
    }

}

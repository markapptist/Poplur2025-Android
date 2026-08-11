package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.res.painterResource
import com.apptists.poplur2025.R
import com.apptists.poplur2025.performance.rememberPoplurRendererState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.Post
import com.apptists.poplur2025.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun ProfileStatsScreen(
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    var posts by remember(profile.id) { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember(profile.id) { mutableStateOf(true) }
    var animationReady by remember(profile.id) { mutableStateOf(false) }

    LaunchedEffect(profile.id) {
        isLoading = true
        animationReady = false
        val fetched = fetchProfileStatsPosts(profile.id)
        posts = fetched.ifEmpty { DemoPostStore.posts }
        isLoading = false
        animationReady = true
    }

    val totalRadiusKm = remember(posts) { posts.sumOf { it.radius }.toFloat() / 1000f }
    val ringGoalKm = remember(totalRadiusKm) {
        max(10_000f, ceil(max(totalRadiusKm, 1f) / 5_000f) * 5_000f)
    }
    val ringProgressTarget = if (animationReady) (totalRadiusKm / ringGoalKm).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = ringProgressTarget,
        animationSpec = tween(durationMillis = 1200),
        label = "profile-stats-radius-progress"
    )
    val animatedRadius by animateFloatAsState(
        targetValue = if (animationReady) totalRadiusKm else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "profile-stats-radius-value"
    )
    val mapReveal by animateFloatAsState(
        targetValue = if (animationReady) 1f else 0f,
        animationSpec = tween(durationMillis = 900, delayMillis = 220),
        label = "profile-stats-map-reveal"
    )
    val dominanceAreas = remember(posts) { ProfileDominanceArea.make(posts) }
    val trophies = remember(posts, dominanceAreas) { ProfileTrophy.make(posts, dominanceAreas, totalRadiusKm) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StatsPink.pageBG)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 18.dp, bottom = 128.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: title + subtitle (left) with the stats sphere on the right — matches iOS.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Stats",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = StatsPink.title
                )
                Text(
                    text = "Radius ownership,\ncity dominance,\nand trophies",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = StatsPink.subtitle,
                    lineHeight = 21.sp
                )
            }
            Image(
                painter = painterResource(R.drawable.stats_sphere),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        Spacer(Modifier.height(22.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color.Black)
            Spacer(Modifier.height(300.dp))
        } else {
            AnimatedRadiusGraph(
                progress = animatedProgress,
                radiusKm = animatedRadius,
                goalKm = ringGoalKm,
                modifier = Modifier.size(260.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                StatCard(R.drawable.stats_ic_posts, "POSTS", StatsPink.accent, posts.size.toString(), StatsPink.postsCard, Modifier.weight(1f))
                StatCard(R.drawable.stats_ic_goal, "GOAL", StatsPink.goalLabel, formatKm(ringGoalKm), StatsPink.goalCard, Modifier.weight(1f))
            }

            Spacer(Modifier.height(28.dp))
            TrophyRoomSection(trophies = trophies)

            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Dominated Areas", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black)
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.06f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("${dominanceAreas.size} zones", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black.copy(alpha = 0.55f))
                }
            }
            Spacer(Modifier.height(14.dp))
            DominanceInfographicMap(
                areas = dominanceAreas,
                reveal = mapReveal,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFEAF6FA))
            )

            Spacer(Modifier.height(16.dp))
            if (dominanceAreas.isEmpty()) {
                Text(
                    text = "No radius areas yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 28.dp)
                )
            } else {
                dominanceAreas.forEach { area ->
                    DominanceAreaRow(area)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

private suspend fun fetchProfileStatsPosts(userId: String): List<Post> {
    if (userId.isBlank()) return emptyList()
    return runCatching {
        FirebaseFirestore.getInstance()
            .collection("posts")
            .whereEqualTo("userID", userId)
            .get()
            .await()
            .documents
            .map { doc ->
                Post(
                    id = doc.id,
                    userId = doc.getString("userID") ?: doc.getString("userId") ?: userId,
                    username = doc.getString("username") ?: "",
                    displayName = doc.getString("displayName") ?: "",
                    mediaUrl = doc.getString("mediaURL") ?: doc.getString("mediaUrl"),
                    videoUrl = doc.getString("videoURL") ?: doc.getString("videoUrl"),
                    latitude = doc.getDouble("latitude"),
                    longitude = doc.getDouble("longitude"),
                    locationName = doc.getString("locationName") ?: "",
                    likes = doc.getLong("likes")?.toInt() ?: 0,
                    comments = doc.getLong("comments")?.toInt() ?: 0,
                    radius = doc.getLong("radius")?.toInt() ?: 0,
                    totalRadius = doc.getLong("totalRadius")?.toInt() ?: 0
                )
            }
            .distinctBy { it.id }
    }.getOrElse { emptyList() }
}

@Composable
private fun AnimatedRadiusGraph(
    progress: Float,
    radiusKm: Float,
    goalKm: Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.11f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            drawArc(
                color = Color.Black.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(
                        Color(0xFF18D8E8),
                        Color(0xFF4DFF58),
                        Color(0xFFFFD91A),
                        Color(0xFFFF8C1A),
                        Color(0xFFFF4FA3),
                        Color(0xFF18D8E8)
                    )
                ),
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(formatKmCompact(radiusKm), fontSize = 46.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text("ACCUMULATED", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black.copy(alpha = 0.5f))
            Text("${(progress * 100).roundToInt()}% of ${formatKmCompact(goalKm)}", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black.copy(alpha = 0.42f))
        }
    }
}

@Composable
private fun StatCard(
    iconRes: Int,
    title: String,
    labelColor: Color,
    value: String,
    background: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(52.dp)
        )
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = labelColor)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = StatsPink.title, maxLines = 1)
        }
    }
}

// iOS "Stats" mockup palette (sampled from ProfileStatsView.swift StatsC).
private object StatsPink {
    val pageBG = Color(0xFFFDEDF3)
    val title = Color(0xFF12131A)
    val subtitle = Color(0xFF7C7A85)
    val accent = Color(0xFFFF2D82)
    val postsCard = Color(0xFFFEEFF6)
    val goalCard = Color(0xFFF2F2FE)
    val goalLabel = Color(0xFF5B5BD6)
}

@Composable
private fun DominanceInfographicMap(
    areas: List<ProfileDominanceArea>,
    reveal: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFDDF4FA), Color(0xFFC2E3F0)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )

            val land = Path().apply {
                moveTo(size.width * 0.14f, size.height * 0.23f)
                cubicTo(size.width * 0.25f, size.height * 0.08f, size.width * 0.42f, size.height * 0.22f, size.width * 0.52f, size.height * 0.14f)
                cubicTo(size.width * 0.66f, size.height * 0.03f, size.width * 0.80f, size.height * 0.16f, size.width * 0.84f, size.height * 0.28f)
                cubicTo(size.width * 0.96f, size.height * 0.43f, size.width * 0.86f, size.height * 0.64f, size.width * 0.73f, size.height * 0.70f)
                cubicTo(size.width * 0.58f, size.height * 0.78f, size.width * 0.44f, size.height * 0.88f, size.width * 0.28f, size.height * 0.82f)
                cubicTo(size.width * 0.10f, size.height * 0.72f, size.width * 0.05f, size.height * 0.44f, size.width * 0.14f, size.height * 0.23f)
                close()
            }
            drawPath(land, Color(0xFFD1E0B8))
            drawPath(land, Color.Black.copy(alpha = 0.12f), style = Stroke(width = 2.dp.toPx()))

            for (index in 1..2) {
                val x = size.width * index / 3f
                drawLine(Color.White.copy(alpha = 0.36f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                val y = size.height * index / 3f
                drawLine(Color.White.copy(alpha = 0.36f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
        }

        areas.forEach { area ->
            DominanceBubble(
                area = area,
                reveal = reveal,
                mapWidth = maxWidth,
                mapHeight = maxHeight
            )
        }

        Text(
            text = "Radius ownership by active zone",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black.copy(alpha = 0.54f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.72f))
                .padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun DominanceBubble(
    area: ProfileDominanceArea,
    reveal: Float,
    mapWidth: androidx.compose.ui.unit.Dp,
    mapHeight: androidx.compose.ui.unit.Dp
) {
    val bubbleSize = (54f + min(area.percent, 60f) * 1.45f).dp
    Box(
        modifier = Modifier
            .padding(
                start = ((mapWidth - bubbleSize) * area.x).coerceAtLeast(0.dp),
                top = ((mapHeight - bubbleSize) * area.y).coerceAtLeast(0.dp)
            )
            .size(bubbleSize)
            .graphicsLayer {
                scaleX = reveal
                scaleY = reveal
                alpha = reveal
            }
            .clip(CircleShape)
            .background(area.color.copy(alpha = 0.74f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("${area.percent.roundToInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text(area.title, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black.copy(alpha = 0.62f), maxLines = 1)
        }
    }
}

@Composable
private fun DominanceAreaRow(area: ProfileDominanceArea) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.035f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(13.dp).clip(CircleShape).background(area.color))
        Column(Modifier.weight(1f)) {
            Text(area.title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text("${area.postCount} posts inside this zone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.48f))
        }
        Text("${area.percent.roundToInt()}%", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black)
    }
}

private data class ProfileDominanceArea(
    val id: String,
    val title: String,
    val percent: Float,
    val postCount: Int,
    val x: Float,
    val y: Float,
    val color: Color
) {
    companion object {
        fun make(posts: List<Post>): List<ProfileDominanceArea> {
            val valid = posts.filter { it.latitude != null && it.longitude != null && (it.latitude != 0.0 || it.longitude != 0.0) }
            if (valid.isEmpty()) return emptyList()

            val totalWeight = max(1f, valid.sumOf { max(it.radius, 1).toDouble() }.toFloat())
            val minLat = valid.minOf { it.latitude ?: 0.0 }
            val maxLat = valid.maxOf { it.latitude ?: minLat }
            val minLon = valid.minOf { it.longitude ?: 0.0 }
            val maxLon = valid.maxOf { it.longitude ?: minLon }
            val latSpan = max((maxLat - minLat).toFloat(), 0.0001f)
            val lonSpan = max((maxLon - minLon).toFloat(), 0.0001f)
            val buckets = mutableMapOf<String, Bucket>()

            valid.forEach { post ->
                val normalizedX = (((post.longitude ?: minLon) - minLon) / lonSpan).toFloat()
                val normalizedY = (1.0 - (((post.latitude ?: minLat) - minLat) / latSpan)).toFloat()
                val col = (normalizedX * 3f).toInt().coerceIn(0, 2)
                val row = (normalizedY * 3f).toInt().coerceIn(0, 2)
                val key = "$row-$col"
                val weight = max(post.radius, 1).toFloat()
                val bucket = buckets.getOrPut(key) { Bucket() }
                bucket.weight += weight
                bucket.postCount += 1
                bucket.x += normalizedX * weight
                bucket.y += normalizedY * weight
            }

            val names = listOf(
                listOf("Northwest", "North", "Northeast"),
                listOf("West", "Central", "East"),
                listOf("Southwest", "South", "Southeast")
            )
            val colors = listOf(
                Color(0xFF18D8E8),
                Color(0xFF4DFF58),
                Color(0xFFFFD91A),
                Color(0xFFFF8C1A),
                Color(0xFFFF4FA3),
                Color(0xFF7076FF)
            )

            return buckets.map { (key, bucket) ->
                val parts = key.split("-").map { it.toInt() }
                val row = parts.getOrElse(0) { 1 }
                val col = parts.getOrElse(1) { 1 }
                ProfileDominanceArea(
                    id = key,
                    title = names[row][col],
                    percent = (bucket.weight / totalWeight) * 100f,
                    postCount = bucket.postCount,
                    x = (bucket.x / max(bucket.weight, 1f)).coerceIn(0.08f, 0.92f),
                    y = (bucket.y / max(bucket.weight, 1f)).coerceIn(0.10f, 0.88f),
                    color = colors[(row * 3 + col) % colors.size]
                )
            }.sortedByDescending { it.percent }
        }
    }

    private class Bucket {
        var weight: Float = 0f
        var postCount: Int = 0
        var x: Float = 0f
        var y: Float = 0f
    }
}

private fun formatKm(km: Float): String = when {
    km >= 1_000_000f -> String.format("%.1fM KM", km / 1_000_000f)
    km >= 1_000f -> String.format("%.0fK KM", km / 1_000f)
    else -> String.format("%.0f KM", km)
}

private fun formatKmCompact(km: Float): String = when {
    km >= 1_000_000f -> String.format("%.1fM", km / 1_000_000f)
    km >= 1_000f -> String.format("%.0fK", km / 1_000f)
    else -> String.format("%.0f", km)
}

// ─────────────────────────────────────────────────────────────────────────────
// MARK: Trophy Room
// ─────────────────────────────────────────────────────────────────────────────

private data class ProfileTrophy(
    val id: String,
    val title: String,
    val lockedHint: String,
    val isUnlocked: Boolean,
    val displayColor: Color,
    val tier: Int
) {
    companion object {
        fun make(posts: List<Post>, dominanceAreas: List<ProfileDominanceArea>, totalRadiusKm: Float): List<ProfileTrophy> {
            val totalLikes = posts.sumOf { it.likes }
            val maxRadius = posts.maxOfOrNull { it.radius } ?: 0
            val uniqueBuckets = posts.mapNotNull { post ->
                val lat = post.latitude ?: return@mapNotNull null
                val lon = post.longitude ?: return@mapNotNull null
                "${(lat * 10).roundToInt()}:${(lon * 10).roundToInt()}"
            }.toSet()
            return listOf(
                ProfileTrophy("first-post",  "First Signal",    "Post once",        posts.isNotEmpty(),             Color(1.0f, 0.73f, 0.22f), 1),
                ProfileTrophy("five-posts",  "Creator Streak",  "Reach 5 posts",    posts.size >= 5,                Color(0.29f, 0.84f, 0.68f), 2),
                ProfileTrophy("radius-100",  "Radius Rookie",   "Reach 100 KM",     totalRadiusKm >= 100f,          Color(0.35f, 0.78f, 1.0f), 1),
                ProfileTrophy("radius-1000", "City Builder",    "Reach 1K KM",      totalRadiusKm >= 1_000f,        Color(1.0f, 0.48f, 0.78f), 3),
                ProfileTrophy("likes-1000",  "Crowd Pull",      "Get 1K likes",     totalLikes >= 1_000,            Color(0.74f, 0.55f, 1.0f), 2),
                ProfileTrophy("wide-radius", "Wide Reach",      "One 250 KM post",  maxRadius >= 250_000,           Color(1.0f, 0.58f, 0.30f), 2),
                ProfileTrophy("three-zones", "Zone Collector",  "Own 3 zones",      dominanceAreas.size >= 3,       Color(0.58f, 0.92f, 0.32f), 3),
                ProfileTrophy("traveler",    "Map Traveler",    "Post in 3 areas",  uniqueBuckets.size >= 3,        Color(0.12f, 0.70f, 0.95f), 2)
            )
        }
    }
}

@Composable
private fun TrophyRoomSection(trophies: List<ProfileTrophy>) {
    val unlockedCount = trophies.count { it.isUnlocked }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Trophy Room", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.Black)
                Text(
                    "Unlocked trophies shine. Locked trophies stay gray.",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.5f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFFFD91A).copy(alpha = 0.18f))
                    .padding(horizontal = 11.dp, vertical = 7.dp)
            ) {
                Text("$unlockedCount/${trophies.size}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
        }

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
        ) {
            TrophyRoomCanvas(trophies = trophies, modifier = Modifier.fillMaxSize())
            Text(
                text = "TROPHY ROOM",
                fontSize = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.28f),
                letterSpacing = 2.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // 2-column trophy legend grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height((trophies.size / 2 * 58).dp)
        ) {
            items(trophies) { trophy ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = if (trophy.isUnlocked) 0.045f else 0.025f))
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (trophy.isUnlocked) trophy.displayColor else Color.Gray.copy(alpha = 0.34f))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            trophy.title,
                            fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black,
                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            if (trophy.isUnlocked) "Unlocked" else trophy.lockedHint,
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.48f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrophyRoomCanvas(trophies: List<ProfileTrophy>, modifier: Modifier = Modifier) {
    var sparkleTime by remember { mutableFloatStateOf(0f) }
    val performance = rememberPoplurRendererState(
        name = "PoplurTrophyParticles",
        heavy = false,
    )
    LaunchedEffect(
        performance.active,
        performance.snapshot.targetFps,
        performance.snapshot.decorativeRenderingAllowed,
    ) {
        if (!performance.active || !performance.snapshot.decorativeRenderingAllowed) return@LaunchedEffect
        val t0 = System.currentTimeMillis()
        while (true) {
            sparkleTime = (System.currentTimeMillis() - t0) / 1000f
            kotlinx.coroutines.delay(performance.snapshot.frameIntervalMillis)
        }
    }

    Canvas(modifier = modifier) {
        val W = size.width
        val H = size.height
        val sX = W / 390f
        val sY = H / 360f

        // Dark purple gradient background
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0.10f, 0.08f, 0.16f), Color(0.05f, 0.04f, 0.10f)),
                startY = 0f, endY = H
            )
        )

        // Shelves (two rows)
        val shelfSpriteKitYs = listOf(248f, 98f)
        for (spY in shelfSpriteKitYs) {
            val shelfY = H - spY * sY - 30f * sY
            val shelfW = (390f - 16f) * sX
            val shelfH = 11f * sY
            val shelfLeft = (W - shelfW) / 2f
            drawRoundRect(
                color = Color(0.22f, 0.17f, 0.10f),
                topLeft = Offset(shelfLeft, shelfY - shelfH / 2f),
                size = Size(shelfW, shelfH),
                cornerRadius = CornerRadius(3f * sX)
            )
            drawRoundRect(
                color = Color(0.70f, 0.55f, 0.30f, 0.35f),
                topLeft = Offset(shelfLeft, shelfY - shelfH / 2f - 2f * sY),
                size = Size(shelfW, 2f * sY),
                cornerRadius = CornerRadius(1f * sX)
            )
        }

        // Draw each trophy
        val rowSpriteKitYs = listOf(248f, 98f)
        trophies.forEachIndexed { i, trophy ->
            val row = i / 4
            val col = i % 4
            if (row >= rowSpriteKitYs.size) return@forEachIndexed
            val cx = (col + 0.5f) * 97.5f * sX
            val basePt = H - rowSpriteKitYs[row] * sY
            val floatOff = if (trophy.isUnlocked) {
                (2.5f + kotlin.math.sin(sparkleTime * (4.1f - i * 0.08f) + i * 0.7f) * 2.5f) * sY
            } else {
                0f
            }
            drawTrophy(cx, basePt - floatOff, sX, sY, trophy, sparkleTime)
        }
    }
}

private fun DrawScope.drawTrophy(
    cx: Float, basePt: Float, sX: Float, sY: Float,
    trophy: ProfileTrophy, sparkTime: Float
) {
    val isUnlocked = trophy.isUnlocked
    val tCol = if (isUnlocked) trophy.displayColor else Color(0.36f, 0.36f, 0.36f)
    val aCol = if (isUnlocked) Color(1f, 0.86f, 0.38f) else Color(0.24f, 0.24f, 0.24f)
    val oa = if (isUnlocked) 1f else 0.65f  // overall alpha

    // Base plate
    val baseW = 50f * sX; val baseH = 10f * sY
    val baseColor = if (isUnlocked) Color(0.12f, 0.10f, 0.08f) else Color(0.18f, 0.18f, 0.18f)
    drawRoundRect(baseColor.copy(alpha = oa), topLeft = Offset(cx - baseW / 2f, basePt - baseH), size = Size(baseW, baseH), cornerRadius = CornerRadius(4f * sX))
    drawRoundRect(aCol.copy(alpha = 0.55f * oa), topLeft = Offset(cx - baseW / 2f, basePt - baseH), size = Size(baseW, baseH), cornerRadius = CornerRadius(4f * sX), style = Stroke(1f * sX))

    // Foot disc
    drawOval(aCol.copy(alpha = oa), topLeft = Offset(cx - 15f * sX, basePt - 14f * sY - 4f * sY), size = Size(30f * sX, 8f * sY))

    // Stem
    val stemH = 18f * sY; val stemW = 8f * sX; val stemTop = basePt - 36f * sY
    drawRoundRect(aCol.copy(alpha = oa), topLeft = Offset(cx - stemW / 2f, stemTop), size = Size(stemW, stemH), cornerRadius = CornerRadius(3f * sX))

    // Cup (trapezoid): narrower at bottom (stemY), wider at top
    val cupBotHalf = 11f * sX; val cupTopHalf = 19f * sX; val cupH = 38f * sY
    val cupBotY = basePt - 37f * sY
    val cupTopY = cupBotY - cupH
    val cupPath = Path().apply {
        moveTo(cx - cupBotHalf, cupBotY)
        lineTo(cx - cupTopHalf, cupTopY)
        lineTo(cx + cupTopHalf, cupTopY)
        lineTo(cx + cupBotHalf, cupBotY)
        close()
    }
    drawPath(cupPath, tCol.copy(alpha = oa))
    drawPath(cupPath, tCol.copy(alpha = (if (isUnlocked) 0.5f else 0.2f) * oa), style = Stroke(1.5f * sX))

    // Inner shine (unlocked only)
    if (isUnlocked) {
        val sw = 6f * sX
        val shinePath = Path().apply {
            moveTo(cx - sw / 2f, cupBotY - 4f * sY)
            lineTo(cx - sw / 2f - 4f * sX, cupTopY + 6f * sY)
            lineTo(cx + sw / 2f - 4f * sX, cupTopY + 6f * sY)
            lineTo(cx + sw / 2f, cupBotY - 4f * sY)
            close()
        }
        drawPath(shinePath, Color.White.copy(alpha = 0.18f))
    }

    // Handles (C-arcs on each side of the cup)
    val hcy = basePt - 56f * sY
    for (xSign in listOf(-1f, 1f)) {
        val hcx = cx + xSign * (cupTopHalf + 5f * sX)
        val r = 10f * sX
        val handlePath = Path().apply {
            if (xSign > 0f) {
                arcTo(Rect(hcx - r, hcy - r, hcx + r, hcy + r), startAngleDegrees = 230f, sweepAngleDegrees = -160f, forceMoveTo = true)
            } else {
                arcTo(Rect(hcx - r, hcy - r, hcx + r, hcy + r), startAngleDegrees = -50f, sweepAngleDegrees = 160f, forceMoveTo = true)
            }
        }
        drawPath(handlePath, tCol.copy(alpha = oa), style = Stroke(width = 4.5f * sX, cap = StrokeCap.Round))
    }

    // Lip (ellipse at cup top)
    val lipW = cupTopHalf * 2f + 8f * sX; val lipH = 9f * sY
    drawOval(aCol.copy(alpha = oa), topLeft = Offset(cx - lipW / 2f, cupTopY - lipH / 2f), size = Size(lipW, lipH))

    // Crown gem
    val crownY = cupTopY - 9f * sY; val crownR = 5.5f * sX
    drawCircle(aCol.copy(alpha = oa), radius = crownR, center = Offset(cx, crownY))
    drawCircle(Color.White.copy(alpha = 0.4f * oa), radius = crownR, center = Offset(cx, crownY), style = Stroke(1f * sX))

    if (isUnlocked) {
        // Glow halo
        val glowCenter = Offset(cx, hcy)
        drawCircle(tCol.copy(alpha = 0.10f), radius = 32f * sX, center = glowCenter)
        drawCircle(tCol.copy(alpha = 0.18f), radius = 32f * sX, center = glowCenter, style = Stroke(1f * sX))

        // Sparkle particles radiating above crown
        val sparkCenter = Offset(cx, crownY - 10f * sY)
        for (p in 0 until 8) {
            val angle = (p * 45f + sparkTime * 80f) * (Math.PI.toFloat() / 180f)
            val r = (18f + 8f * sin(sparkTime * 1.5f + p * 0.9f)) * sX
            val px = sparkCenter.x + r * cos(angle)
            val py = sparkCenter.y + r * sin(angle)
            val alpha = (0.3f + 0.5f * sin(sparkTime * 2.0f + p * 1.1f)).coerceIn(0f, 1f)
            drawCircle(Color.White.copy(alpha = alpha), radius = 2.5f * sX, center = Offset(px, py))
        }
    }
}

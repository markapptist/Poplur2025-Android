package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.apptists.poplur2025.billing.PoplurBillingManager
import com.apptists.poplur2025.billing.PoplurProducts
import com.apptists.poplur2025.billing.PurchaseEventStatus

import com.apptists.poplur2025.R
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.BusinessVerificationStatus
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.CubeSkinBackgroundLayer
import com.apptists.poplur2025.ui.components.ProfileAvatarSpeaker
import com.apptists.poplur2025.ui.components.PerformanceSceneView
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay
import com.apptists.poplur2025.ui.components.poplurCountdownDisplayModifier
import com.apptists.poplur2025.ui.components.poplurRadiusDisplayModifier
import com.apptists.poplur2025.ui.profile.ProfileStatusBarIcons
import com.apptists.poplur2025.ui.profile.ProfileTheme
import com.apptists.poplur2025.ui.profile.ProfileThemes
import com.apptists.poplur2025.ui.uiTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.node.ModelNode
import kotlin.math.abs
import kotlin.math.sin

private enum class ProfilePanel {
    Home,
    Notifications,
    Phone,
    Store,
    Stats,
    Creator
}

private data class PoplurCreditCardProduct(
    val id: String,
    val name: String,
    val price: String,
    val material: String,
    val delivery: String,
    val accent: Color,
    val frameResIds: List<Int>
)

private val PoplurCreditCardCatalog = listOf(
    PoplurCreditCardProduct(
        id = "cosmic-silver",
        name = "Cosmic Silver",
        price = "$29",
        material = "star-polished graphite",
        delivery = "Ships in 3-5 days",
        accent = Color(0xFF8F9499),
        frameResIds = listOf(
            R.drawable.poplur_card_cosmic_silver_frame_01,
            R.drawable.poplur_card_cosmic_silver_frame_02,
            R.drawable.poplur_card_cosmic_silver_frame_03,
            R.drawable.poplur_card_cosmic_silver_frame_04,
            R.drawable.poplur_card_cosmic_silver_frame_05,
            R.drawable.poplur_card_cosmic_silver_frame_06,
            R.drawable.poplur_card_cosmic_silver_frame_07,
            R.drawable.poplur_card_cosmic_silver_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "icy-aqua",
        name = "Icy Aqua",
        price = "$29",
        material = "turquoise crystal",
        delivery = "Ships in 3-5 days",
        accent = Color(0xFF0DA8A8),
        frameResIds = listOf(
            R.drawable.poplur_card_icy_aqua_frame_01,
            R.drawable.poplur_card_icy_aqua_frame_02,
            R.drawable.poplur_card_icy_aqua_frame_03,
            R.drawable.poplur_card_icy_aqua_frame_04,
            R.drawable.poplur_card_icy_aqua_frame_05,
            R.drawable.poplur_card_icy_aqua_frame_06,
            R.drawable.poplur_card_icy_aqua_frame_07,
            R.drawable.poplur_card_icy_aqua_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "champagne-gold",
        name = "Champagne Gold",
        price = "$35",
        material = "gold glitter resin",
        delivery = "Ships in 2-4 days",
        accent = Color(0xFFDB8C1F),
        frameResIds = listOf(
            R.drawable.poplur_card_champagne_gold_frame_01,
            R.drawable.poplur_card_champagne_gold_frame_02,
            R.drawable.poplur_card_champagne_gold_frame_03,
            R.drawable.poplur_card_champagne_gold_frame_04,
            R.drawable.poplur_card_champagne_gold_frame_05,
            R.drawable.poplur_card_champagne_gold_frame_06,
            R.drawable.poplur_card_champagne_gold_frame_07,
            R.drawable.poplur_card_champagne_gold_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "jet-black",
        name = "Jet Black Onyx",
        price = "$35",
        material = "black starfield",
        delivery = "Ships in 2-4 days",
        accent = Color(0xFF101113),
        frameResIds = listOf(
            R.drawable.poplur_card_jet_black_onyx_frame_01,
            R.drawable.poplur_card_jet_black_onyx_frame_02,
            R.drawable.poplur_card_jet_black_onyx_frame_03,
            R.drawable.poplur_card_jet_black_onyx_frame_04,
            R.drawable.poplur_card_jet_black_onyx_frame_05,
            R.drawable.poplur_card_jet_black_onyx_frame_06,
            R.drawable.poplur_card_jet_black_onyx_frame_07,
            R.drawable.poplur_card_jet_black_onyx_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "amethyst",
        name = "Amethyst Purple",
        price = "$39",
        material = "faceted crystal",
        delivery = "Limited finish",
        accent = Color(0xFF7524C2),
        frameResIds = listOf(
            R.drawable.poplur_card_amethyst_purple_frame_01,
            R.drawable.poplur_card_amethyst_purple_frame_02,
            R.drawable.poplur_card_amethyst_purple_frame_03,
            R.drawable.poplur_card_amethyst_purple_frame_04,
            R.drawable.poplur_card_amethyst_purple_frame_05,
            R.drawable.poplur_card_amethyst_purple_frame_06,
            R.drawable.poplur_card_amethyst_purple_frame_07,
            R.drawable.poplur_card_amethyst_purple_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "ruby",
        name = "Ruby Red",
        price = "$39",
        material = "ruby sparkle",
        delivery = "Limited finish",
        accent = Color(0xFFB80514),
        frameResIds = listOf(
            R.drawable.poplur_card_ruby_red_frame_01,
            R.drawable.poplur_card_ruby_red_frame_02,
            R.drawable.poplur_card_ruby_red_frame_03,
            R.drawable.poplur_card_ruby_red_frame_04,
            R.drawable.poplur_card_ruby_red_frame_05,
            R.drawable.poplur_card_ruby_red_frame_06,
            R.drawable.poplur_card_ruby_red_frame_07,
            R.drawable.poplur_card_ruby_red_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "emerald",
        name = "Emerald Green",
        price = "$39",
        material = "emerald glitter",
        delivery = "Limited finish",
        accent = Color(0xFF007334),
        frameResIds = listOf(
            R.drawable.poplur_card_emerald_green_frame_01,
            R.drawable.poplur_card_emerald_green_frame_02,
            R.drawable.poplur_card_emerald_green_frame_03,
            R.drawable.poplur_card_emerald_green_frame_04,
            R.drawable.poplur_card_emerald_green_frame_05,
            R.drawable.poplur_card_emerald_green_frame_06,
            R.drawable.poplur_card_emerald_green_frame_07,
            R.drawable.poplur_card_emerald_green_frame_08
        )
    ),
    PoplurCreditCardProduct(
        id = "sapphire",
        name = "Sapphire Blue",
        price = "$39",
        material = "sapphire crystal",
        delivery = "Limited finish",
        accent = Color(0xFF0830B8),
        frameResIds = listOf(
            R.drawable.poplur_card_sapphire_blue_frame_01,
            R.drawable.poplur_card_sapphire_blue_frame_02,
            R.drawable.poplur_card_sapphire_blue_frame_03,
            R.drawable.poplur_card_sapphire_blue_frame_04,
            R.drawable.poplur_card_sapphire_blue_frame_05,
            R.drawable.poplur_card_sapphire_blue_frame_06,
            R.drawable.poplur_card_sapphire_blue_frame_07,
            R.drawable.poplur_card_sapphire_blue_frame_08
        )
    )
)

@Composable
fun ProfileScreen(
    profile: UserProfile = UserProfile(),
    authViewModel: AuthViewModel? = null,
    onOpenSettings: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    cubeMenuCommand: ProfileCubeMenuCommand? = null,
    onProfilePanelIconChanged: (String?) -> Unit = {},
    onPhoneCallVisibilityChanged: (Boolean) -> Unit = {},
    onColorwaySelected: (Color) -> Unit = {},
    forcedColorway: Color? = null,
    avatarIs3D: Boolean = false,
    cubeSkin: CubeSkin = CubeSkin.Default,
    profileTheme: ProfileTheme = ProfileThemes.Default
) {
    ProfileScreenContent(
        profile = profile,
        onOpenSettings = onOpenSettings,
        onOpenMessages = onOpenMessages,
        cubeMenuCommand = cubeMenuCommand,
        onProfilePanelIconChanged = onProfilePanelIconChanged,
        onPhoneCallVisibilityChanged = onPhoneCallVisibilityChanged,
        onColorwaySelected = onColorwaySelected,
        forcedColorway = forcedColorway,
        avatarIs3D = avatarIs3D,
        cubeSkin = cubeSkin,
        profileTheme = profileTheme
    )
}

@Composable
fun ProfileScreenContent(
    profile: UserProfile = UserProfile(),
    onOpenSettings: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    cubeMenuCommand: ProfileCubeMenuCommand? = null,
    onProfilePanelIconChanged: (String?) -> Unit = {},
    onPhoneCallVisibilityChanged: (Boolean) -> Unit = {},
    onColorwaySelected: (Color) -> Unit = {},
    forcedColorway: Color? = null,
    avatarIs3D: Boolean = false,
    cubeSkin: CubeSkin = CubeSkin.Default,
    profileTheme: ProfileTheme = ProfileThemes.Default
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentProfile by remember { mutableStateOf(profile) }
    var isEditing by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(ProfilePanel.Home) }
    var hasUncheckedNotifications by remember { mutableStateOf(true) }
    var purchasedRadius by remember { mutableIntStateOf(0) }

    LaunchedEffect(cubeMenuCommand?.id) {
        when (cubeMenuCommand?.action) {
            ProfileCubeMenuAction.Phone -> activePanel = ProfilePanel.Phone
            ProfileCubeMenuAction.Notifications -> {
                hasUncheckedNotifications = false
                activePanel = ProfilePanel.Notifications
            }
            ProfileCubeMenuAction.Store -> activePanel = ProfilePanel.Store
            ProfileCubeMenuAction.Stats -> activePanel = ProfilePanel.Stats
            ProfileCubeMenuAction.Creator -> activePanel = ProfilePanel.Creator
            ProfileCubeMenuAction.GoHome -> activePanel = ProfilePanel.Home
            null -> Unit
        }
    }

    LaunchedEffect(activePanel) {
        onProfilePanelIconChanged(
            when (activePanel) {
                ProfilePanel.Notifications -> "Alerts"
                ProfilePanel.Phone -> "Phone"
                ProfilePanel.Store -> "Store"
                ProfilePanel.Stats -> "Stats"
                ProfilePanel.Creator -> "Credits"
                ProfilePanel.Home -> null
            }
        )
    }

    if (isEditing) {
        EditProfileScreen(
            profile = currentProfile,
            onSave = { updated, _ -> 
                currentProfile = updated
                isEditing = false 
            },
            onDismiss = { isEditing = false }
        )
    } else when (activePanel) {
        ProfilePanel.Notifications -> ProfileNotificationsPanel(
            onBack = { activePanel = ProfilePanel.Home },
            onMarkRead = { hasUncheckedNotifications = false }
        )
        ProfilePanel.Phone -> PopularPhoneCallScreen(
            userName = "Poplur Phone",
            onHangUp = { activePanel = ProfilePanel.Home },
            onVisibilityChanged = onPhoneCallVisibilityChanged,
            onMiddleButtonClick = { activePanel = ProfilePanel.Home },
            onColorwaySelected = onColorwaySelected,
            forcedColorway = forcedColorway
        )
        ProfilePanel.Store -> ProfileStorePanel(
            purchasedRadius = purchasedRadius,
            onPurchase = { purchasedRadius += it },
            onBack = { activePanel = ProfilePanel.Home }
        )
        ProfilePanel.Stats -> ProfileStatsScreen(profile = currentProfile)
        ProfilePanel.Creator -> CreateEntryScreen(
            onOriginalContent = { activePanel = ProfilePanel.Home },
            onPoplurAI = { activePanel = ProfilePanel.Home },
            onBack = { activePanel = ProfilePanel.Home }
        )
        ProfilePanel.Home -> ProfileHome(
            profile = currentProfile,
            selectedTab = selectedTab,
            onTab = { selectedTab = it },
            onOpenSettings = onOpenSettings,
            onOpenMessages = onOpenMessages,
            hasUncheckedNotifications = hasUncheckedNotifications,
            onOpenNotifications = {
                hasUncheckedNotifications = false
                activePanel = ProfilePanel.Notifications
            },
            onOpenPhone = { activePanel = ProfilePanel.Phone },
            onOpenStore = { activePanel = ProfilePanel.Store },
            onEdit = { isEditing = true },
            avatarIs3D = avatarIs3D,
            cubeSkin = cubeSkin,
            profileTheme = profileTheme
        )
    }
}

@Composable
private fun ProfileHome(
    profile: UserProfile,
    selectedTab: Int,
    onTab: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMessages: () -> Unit,
    hasUncheckedNotifications: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenPhone: () -> Unit,
    onOpenStore: () -> Unit,
    onEdit: () -> Unit,
    avatarIs3D: Boolean = false,
    cubeSkin: CubeSkin = CubeSkin.Default,
    profileTheme: ProfileTheme = ProfileThemes.Default
) {
    val cubeTheme = cubeSkin.uiTheme()
    val accentColor = if (profileTheme.isDefault) cubeTheme.accent else profileTheme.primaryAccent
    val foregroundColor = if (profileTheme.isDefault) Color.Black else profileTheme.foreground
    val localView = LocalView.current
    var showNotificationBanner by remember { mutableStateOf(false) }
    var localUnread by remember { mutableStateOf(hasUncheckedNotifications) }
    val mediaPosts = DemoPostStore.posts
    val profilePostCount = mediaPosts.size
    val profileTotalRadius = mediaPosts.sumOf { it.radius.coerceAtLeast(0) }

    DisposableEffect(profileTheme.id, localView) {
        if (profileTheme.isDefault) return@DisposableEffect onDispose { }
        val activity = localView.context.findActivity()
        val controller = activity?.let { WindowCompat.getInsetsController(it.window, localView) }
        val previousStatusBarColor = activity?.window?.statusBarColor
        val previousLightIcons = controller?.isAppearanceLightStatusBars
        if (activity != null && controller != null) {
            activity.window.statusBarColor = profileTheme.overlayColor.toArgb()
            controller.isAppearanceLightStatusBars =
                profileTheme.statusBarIcons == ProfileStatusBarIcons.Dark
        }
        onDispose {
            if (activity != null && previousStatusBarColor != null) {
                activity.window.statusBarColor = previousStatusBarColor
            }
            if (controller != null && previousLightIcons != null) {
                controller.isAppearanceLightStatusBars = previousLightIcons
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
    ) {
        if (profileTheme.isDefault) {
            CubeSkinBackgroundLayer(
                skin = cubeSkin,
                opacity = 0.16f,
                baseColor = Color.White,
                modifier = Modifier.matchParentSize()
            )
        } else {
            profileTheme.backgroundResId?.let { backgroundResId ->
                Image(
                    painter = painterResource(backgroundResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    Modifier
                        .matchParentSize()
                        .background(profileTheme.overlayColor.copy(alpha = profileTheme.overlayOpacity))
                )
            } ?: CubeSkinBackgroundLayer(
                skin = cubeSkin,
                opacity = 0.16f,
                baseColor = Color.White,
                modifier = Modifier.matchParentSize()
            )
        }

        Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileAvatarSpeaker(
                avatarUrl = profile.avatarUrl,
                totalRadiusMeters = profileTotalRadius,
                postCount = profilePostCount,
                size = if (avatarIs3D) 180.dp else 96.dp,
                is3D = avatarIs3D,
                accentColor = accentColor,
                cubeSkin = cubeSkin
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.profileDisplayName.uppercase(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = foregroundColor
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = foregroundColor.copy(alpha = 0.72f),
                    modifier = Modifier.size(20.dp).clickable { onEdit() }
                )
            }
            BusinessProfileBadge(profile = profile, contentColor = foregroundColor)
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.Center) {
                ProfileTabItem(
                    "Media",
                    selectedTab == 0,
                    cubeSkin = cubeSkin,
                    contentColor = foregroundColor,
                    accentColor = accentColor
                ) { onTab(0) }
                Spacer(Modifier.width(56.dp))
                ProfileTabItem(
                    "Messages",
                    isSelected = false,
                    cubeSkin = cubeSkin,
                    contentColor = foregroundColor,
                    accentColor = accentColor,
                    onClick = onOpenMessages
                )
            }
            Spacer(Modifier.height(18.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(mediaPosts) { post ->
                    MediaItem(
                        url = post.mediaUrl,
                        radius = post.radius,
                        cubeSkin = cubeSkin,
                        isVideo = post.videoUrl != null
                    )
                }
            }
        }

        TrendingDisplay(
            postsCount = profilePostCount,
            radiusChange = 0,
            contentColor = accentColor,
            modifier = poplurCountdownDisplayModifier()
        )

        RadiusDisplay(
            modifier = poplurRadiusDisplayModifier(),
            radius = 56,
            unit = "mi",
            contentColor = accentColor
        )

        // Notification bell (top-right) + pink drop-down banner — matches iOS ProfileNotificationBanner
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 10.dp, end = 18.dp)
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showNotificationBanner = !showNotificationBanner
                    if (showNotificationBanner) {
                        localUnread = false
                        onOpenNotifications()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text("🔔", fontSize = 22.sp)
            if (localUnread) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }

        // Tap-outside scrim to dismiss the banner
        if (showNotificationBanner) {
            Box(
                Modifier
                    .matchParentSize()
                    .zIndex(9_999f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showNotificationBanner = false }
            )
        }

        AnimatedVisibility(
            visible = showNotificationBanner,
            enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.36f,
                    stiffness = Spring.StiffnessLow
                ),
                initialOffsetY = { fullHeight -> -fullHeight }
            ) + fadeIn(animationSpec = tween(durationMillis = 220)),
            exit = slideOutVertically(
                animationSpec = spring(
                    dampingRatio = 0.58f,
                    stiffness = Spring.StiffnessLow
                ),
                targetOffsetY = { fullHeight -> -fullHeight }
            ) + fadeOut(
                animationSpec = tween(
                    durationMillis = 340,
                    delayMillis = 500
                )
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10_000f)
        ) {
            ProfileNotificationBanner()
        }
    }
}

/** Pink notification banner that drops from the top — matches iOS ProfileNotificationBanner. */
@Composable
private fun ProfileNotificationBanner() {
    data class Alert(
        val title: String,
        val detail: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val color: Color,
        val unread: Boolean
    )
    val alerts = listOf(
        Alert("Post liked", "Your latest post picked up new likes.", Icons.Default.Email, Color(0xFF3E63DD), true),
        Alert("Post radius jumped", "One of your posts expanded significantly in the last hour.", Icons.Default.CardGiftcard, Color(0xFF30B865), true),
        Alert("Accumulated radius dipped", "Your total radius is down from the previous check.", Icons.Default.CardGiftcard, Color(0xFFFF9F0A), false)
    )

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFF73BB), Color(0xFFF53798), Color(0xFFD91D78))
                )
            )
    ) {
        ProfileNotificationDotMap(Modifier.matchParentSize())
        Column {
            Text(
                "Notifications",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 14.dp)
            )
            Column(
                Modifier
                    .heightIn(max = 340.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                alerts.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF850143).copy(alpha = 0.3f))
                            .border(1.dp, Color(0xFFFFE9F6).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        BadgedBox(
                            badge = { if (item.unread) Badge(containerColor = Color(0xFFFFD1E8)) }
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(
                                item.detail,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFF4FA).copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileNotificationDotMap(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "profileNotificationDotMap")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_800, easing = LinearEasing), RepeatMode.Restart),
        label = "profileNotificationDotMapClock"
    )

    Canvas(modifier) {
        val spacing = (size.width / 52f).coerceIn(6.5f, 10f)
        val rows = (size.height / spacing).toInt()
        val columns = (size.width / spacing).toInt()
        for (row in 0..rows) {
            for (column in 0..columns) {
                val hash = abs(row * 73 + column * 151 + row * column * 19) % 41
                if (hash >= 9) continue
                val phase = (hash * 0.77f + row * 0.19f + column * 0.31f)
                val pulse = (0.5f + 0.5f * sin((clock * 6.28318f + phase).toDouble()).toFloat())
                val radius = (spacing * 0.21f).coerceAtLeast(1.55f) * (0.86f + pulse * 0.28f)
                val center = Offset((column + 0.5f) * spacing, (row + 0.5f) * spacing)
                drawCircle(Color(0xFF78063C).copy(alpha = 0.3f + pulse * 0.22f), radius, center + Offset(0f, 1.3f))
                drawCircle(Color(0xFFFFC4E0).copy(alpha = 0.3f + pulse * 0.34f), radius, center + Offset(0f, -0.8f))
                drawCircle(Color(0xFFFF74BA).copy(alpha = 0.5f + pulse * 0.4f), radius * 0.94f, center)
                if (pulse > 0.9f) drawCircle(Color.White.copy(alpha = (pulse - 0.9f) * 1.6f), radius * 2.4f, center)
            }
        }
    }
}

@Composable
private fun BusinessProfileBadge(profile: UserProfile, contentColor: Color = Color.Black) {
    val business = profile.businessProfile ?: return
    val color = businessStatusColor(business.verificationStatus)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = if (business.verificationStatus == BusinessVerificationStatus.Verified) {
                "Verified Business"
            } else {
                business.verificationStatus.label
            },
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(color.copy(alpha = 0.18f))
                .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
        val subline = listOf(business.category, business.address)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
        if (subline.isNotBlank()) {
            Text(
                text = subline,
                color = contentColor.copy(alpha = 0.72f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

private fun businessStatusColor(status: BusinessVerificationStatus): Color =
    when (status) {
        BusinessVerificationStatus.NotVerified -> Color(0xFF8E8E93)
        BusinessVerificationStatus.PendingReview -> Color(0xFFFF9500)
        BusinessVerificationStatus.Verified -> Color(0xFF34C759)
        BusinessVerificationStatus.Rejected -> Color(0xFFFF3B30)
        BusinessVerificationStatus.NeedsMoreInfo -> Color(0xFF007AFF)
    }

@Composable
private fun ProfileHeaderActionIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    showBadge: Boolean = false
) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)) {
        BadgedBox(
            badge = {
                if (showBadge) {
                    Badge(containerColor = Color.Red)
                }
            }
        ) {
            Icon(
                icon,
                contentDescription,
                tint = Color.DarkGray,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ProfileNotificationsPanel(
    onBack: () -> Unit,
    onMarkRead: () -> Unit
) {
    LaunchedEffect(Unit) {
        onMarkRead()
    }

    val items = listOf(
        Triple("Post liked", "Your latest post picked up new likes.", Icons.Default.Email),
        Triple("Post radius jumped", "One of your posts expanded significantly in the last hour.", Icons.Default.CardGiftcard),
        Triple("Accumulated radius dipped", "Your total radius is down from the previous check.", Icons.Default.CardGiftcard)
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Notifications", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
        Spacer(Modifier.height(18.dp))
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                BadgedBox(
                    badge = {
                        if (index < 2) Badge(containerColor = Color.Red)
                    }
                ) {
                    Surface(shape = CircleShape, color = Color(0xFFEFF4FF), modifier = Modifier.size(48.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(item.third, null, tint = Color(0xFF3E63DD), modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(item.first, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text(item.second, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.58f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileStorePanel(
    purchasedRadius: Int,
    onPurchase: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val billing = remember(context) { PoplurBillingManager.getInstance(context) }
    val productDetails by billing.productDetails.collectAsState()
    val billingMessage by billing.message.collectAsState()
    val purchasedDelta by billing.purchasedRadiusDelta.collectAsState()
    val purchasedCredits by billing.creditsDelta.collectAsState()
    val products = PoplurProducts.cardPackages
    val bag = remember { mutableStateMapOf<String, Int>() }
    var activeIndex by rememberSaveable { mutableIntStateOf(0) }
    var deliveredDelta by remember { mutableIntStateOf(0) }
    var checkoutQueue by remember { mutableStateOf<List<String>>(emptyList()) }
    var checkoutPosition by remember { mutableIntStateOf(0) }
    var checkoutInProgress by remember { mutableStateOf(false) }
    var activeCheckoutProductId by remember { mutableStateOf<String?>(null) }
    var checkoutMessage by remember { mutableStateOf<String?>(null) }

    val activeProduct = products[activeIndex.coerceIn(products.indices)]
    val bagCount = bag.values.sum()
    val allBagProductsAvailable = bag.keys.all(productDetails::containsKey)

    LaunchedEffect(Unit) {
        billing.connect()
        while (true) {
            delay(3_000)
            activeIndex = (activeIndex + 1) % products.size
        }
    }
    LaunchedEffect(purchasedDelta) {
        if (purchasedDelta > deliveredDelta) {
            onPurchase(purchasedDelta - deliveredDelta)
            deliveredDelta = purchasedDelta
        }
    }
    LaunchedEffect(billing) {
        billing.purchaseEvents.collect { event ->
            if (!checkoutInProgress || event.productId != activeCheckoutProductId) {
                return@collect
            }
            when (event.status) {
                PurchaseEventStatus.Success -> {
                    val remaining = (bag[event.productId] ?: 1) - 1
                    if (remaining > 0) bag[event.productId] = remaining else bag.remove(event.productId)
                    val nextPosition = checkoutPosition + 1
                    if (nextPosition >= checkoutQueue.size) {
                        checkoutInProgress = false
                        activeCheckoutProductId = null
                        checkoutQueue = emptyList()
                        checkoutPosition = 0
                        checkoutMessage = "Purchase complete. Verified Poplur Credits are ready to use."
                    } else {
                        checkoutPosition = nextPosition
                        val nextProductId = checkoutQueue[nextPosition]
                        activeCheckoutProductId = nextProductId
                        checkoutMessage = "Verified ${nextPosition} of ${checkoutQueue.size}. Opening the next item…"
                        delay(250)
                        if (activity == null || !billing.launchPurchase(activity, nextProductId)) {
                            checkoutInProgress = false
                            activeCheckoutProductId = null
                            checkoutMessage = "The next item could not be opened in Google Play. Remaining items are still in your bag."
                        }
                    }
                }
                PurchaseEventStatus.Pending -> {
                    checkoutInProgress = false
                    activeCheckoutProductId = null
                    checkoutMessage = "Payment is pending in Google Play. Credits will arrive after payment completes; other items remain in your bag."
                }
                PurchaseEventStatus.Cancelled -> {
                    checkoutInProgress = false
                    activeCheckoutProductId = null
                    checkoutMessage = "Checkout cancelled. Unpurchased items are still in your bag."
                }
                PurchaseEventStatus.Failed -> {
                    checkoutInProgress = false
                    activeCheckoutProductId = null
                    checkoutMessage = event.message
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("Back", color = Color.Black, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Poplur Store", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.Black)
                Text(
                    "Choose a card. Every verified purchase adds Poplur Credits.",
                    color = Color.Black.copy(alpha = 0.58f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            BadgedBox(
                badge = {
                    if (bagCount > 0) Badge { Text(bagCount.toString()) }
                }
            ) {
                Surface(shape = CircleShape, color = Color.Black, modifier = Modifier.size(44.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Bag", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.linearGradient(
                        listOf(cardPalette(activeProduct.cardId).first.copy(alpha = 0.16f), Color(0xFFF5F5F7))
                    )
                )
                .padding(16.dp)
        ) {
            RotatingStoreCardModel(
                product = activeProduct,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.52f)
                    .shadow(20.dp, RoundedCornerShape(22.dp), clip = false)
                    .clip(RoundedCornerShape(22.dp))
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(activeProduct.title, fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Spacer(Modifier.height(7.dp))
                    Text(
                        activeProduct.description,
                        color = Color.Black.copy(alpha = 0.64f),
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    billing.displayPrice(activeProduct.productId, activeProduct.fallbackPrice),
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreBadge("${activeProduct.credits} credits")
                StoreBadge("Google Play")
                StoreBadge("Instant after verification")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                products.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == activeIndex) 9.dp else 7.dp)
                            .clip(CircleShape)
                            .background(if (index == activeIndex) Color.Black else Color.Black.copy(alpha = 0.22f))
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Cards", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(12.dp))
        products.chunked(2).forEach { rowProducts ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowProducts.forEach { product ->
                    StoreCardProductTile(
                        product = product,
                        price = billing.displayPrice(product.productId, product.fallbackPrice),
                        selected = product.productId == activeProduct.productId,
                        quantity = bag[product.productId] ?: 0,
                        onSelect = { activeIndex = products.indexOf(product) },
                        onAdd = {
                            activeIndex = products.indexOf(product)
                            bag[product.productId] = ((bag[product.productId] ?: 0) + 1).coerceAtMost(10)
                        },
                        onRemove = {
                            val next = (bag[product.productId] ?: 0) - 1
                            if (next > 0) bag[product.productId] = next else bag.remove(product.productId)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowProducts.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        StoreBagSummary(
            bag = bag,
            billing = billing,
            checkoutInProgress = checkoutInProgress,
            productsAvailable = allBagProductsAvailable,
            message = checkoutMessage,
            onCheckout = {
                val queue = products.flatMap { product ->
                    List(bag[product.productId] ?: 0) { product.productId }
                }
                if (activity == null) {
                    checkoutMessage = "Google Play checkout is unavailable on this screen."
                } else if (queue.isEmpty()) {
                    checkoutMessage = "Add a card to your bag first."
                } else {
                    checkoutQueue = queue
                    checkoutPosition = 0
                    checkoutInProgress = true
                    activeCheckoutProductId = queue.first()
                    checkoutMessage = "Opening item 1 of ${queue.size} in Google Play…"
                    if (!billing.launchPurchase(activity, queue.first())) {
                        checkoutInProgress = false
                        activeCheckoutProductId = null
                    }
                }
            }
        )

        Spacer(Modifier.height(26.dp))
        Text("Add radius", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(6.dp))
        Text("Boost your Poplur reach after choosing a card.", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
        if (purchasedRadius > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Purchased radius: ${purchasedRadius / 1000} km", color = Color(0xFF178A3D), fontWeight = FontWeight.Black)
        }
        billingMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFF178A3D), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        PoplurProducts.radiusPackages.forEach { pkg ->
            val available = productDetails.containsKey(pkg.productId)
            StoreCreditRow(
                icon = Icons.Default.AttachMoney,
                iconTint = Color(0xFF178A3D),
                iconBackground = Color(0xFFE9F8EE),
                title = pkg.title,
                subtitle = "+${pkg.radiusMeters / 1000} km",
                price = billing.displayPrice(pkg.productId, pkg.fallbackPrice),
                available = available,
                onBuy = { activity?.let { billing.launchPurchase(it, pkg.productId) } }
            )
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(14.dp))
        Text("Creator Credits", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(6.dp))
        Text("For AI images, video, and voice generation.", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
        if (purchasedCredits > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Purchased credits this session: $purchasedCredits", color = Color(0xFFE0248C), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        PoplurProducts.creditPackages.forEach { pkg ->
            val available = productDetails.containsKey(pkg.productId)
            StoreCreditRow(
                icon = Icons.Default.Bolt,
                iconTint = Color(0xFFE0248C),
                iconBackground = Color(0xFFFCE3F0),
                title = pkg.title,
                subtitle = "${pkg.credits} credits",
                price = billing.displayPrice(pkg.productId, pkg.fallbackPrice),
                available = available,
                onBuy = { activity?.let { billing.launchPurchase(it, pkg.productId) } }
            )
            Spacer(Modifier.height(12.dp))
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun RotatingStoreCardModel(
    product: PoplurProducts.CardCreditPackage,
    modifier: Modifier = Modifier
) {
    val palette = cardPalette(product.cardId)
    Box(
        modifier = modifier
            .background(Brush.linearGradient(listOf(palette.first, palette.second)))
            .semantics { contentDescription = "Rotating 3D preview of ${product.title}" },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                PerformanceSceneView(context).apply {
                    scene.skybox = Skybox.Builder()
                        .color(0.055f, 0.045f, 0.065f, 1f)
                        .build(engine)
                    indirectLight = IndirectLight.Builder()
                        .irradiance(1, floatArrayOf(1.05f, 1.0f, 1.08f))
                        .intensity(70_000f)
                        .build(engine)
                    cameraNode.position = Float3(0f, 0f, 3.35f)
                }
            },
            update = { sceneView ->
                if (sceneView.tag == product.productId) return@AndroidView
                runCatching {
                    sceneView.childNodes = emptyList()
                    sceneView.modelLoader.clear()
                    val instance = sceneView.modelLoader.createModelInstance(product.modelAssetPath)
                    val model = ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 1.9f,
                        centerOrigin = Float3(0f, 0f, 0f)
                    )
                    model.rotation = Float3(product.presentationRotationX, 0f, 0f)
                    sceneView.childNodes = listOf(model)
                    sceneView.cameraNode.lookAt(model)
                    var firstFrameNanos = Long.MIN_VALUE
                    sceneView.onFrame = { frameTimeNanos ->
                        if (firstFrameNanos == Long.MIN_VALUE) firstFrameNanos = frameTimeNanos
                        val elapsedSeconds = (frameTimeNanos - firstFrameNanos) / 1_000_000_000f
                        model.rotation = Float3(
                            product.presentationRotationX,
                            (elapsedSeconds * 24f) % 360f,
                            sin(elapsedSeconds * 0.7f) * 2.2f
                        )
                    }
                    sceneView.tag = product.productId
                }.onFailure { error ->
                    sceneView.tag = product.productId
                    Log.e("ProfileStore", "Unable to load ${product.modelAssetPath}", error)
                }
            },
            onRelease = { it.destroy() }
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.56f))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("3D • auto rotates", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StoreCardProductTile(
    product: PoplurProducts.CardCreditPackage,
    price: String,
    selected: Boolean,
    quantity: Int,
    onSelect: () -> Unit,
    onAdd: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = cardPalette(product.cardId)
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = if (selected) 0.075f else 0.035f))
            .border(2.dp, if (selected) palette.first else Color.Transparent, shape)
            .clickable(onClick = onSelect)
            .padding(12.dp)
    ) {
        CardPoster(product, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(product.title, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.Black, maxLines = 2)
        Text("${product.credits} Poplur Credits", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.56f))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(price, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Spacer(Modifier.weight(1f))
            if (quantity > 0) {
                SmallBagButton("−", "Remove one ${product.title}", onRemove)
                Text(quantity.toString(), modifier = Modifier.padding(horizontal = 7.dp), fontWeight = FontWeight.Black)
            }
            SmallBagButton("+", "Add ${product.title} to bag", onAdd)
        }
    }
}

@Composable
private fun CardPoster(product: PoplurProducts.CardCreditPackage, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(cardPosterRes(product.cardId)),
        contentDescription = "${product.title} rendered preview",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1.52f)
            .clip(RoundedCornerShape(14.dp))
    )
}

@Composable
private fun SmallBagButton(label: String, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black,
        modifier = Modifier.size(32.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Black, modifier = Modifier.semantics {
                contentDescription = description
            })
        }
    }
}

@Composable
private fun StoreBagSummary(
    bag: Map<String, Int>,
    billing: PoplurBillingManager,
    checkoutInProgress: Boolean,
    productsAvailable: Boolean,
    message: String?,
    onCheckout: () -> Unit
) {
    val bagItems = PoplurProducts.cardPackages.filter { (bag[it.productId] ?: 0) > 0 }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.Black.copy(alpha = 0.045f))
            .padding(16.dp)
    ) {
        Text("Your bag", fontSize = 19.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        if (bagItems.isEmpty()) {
            Text("No cards added yet.", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
        } else {
            bagItems.forEach { product ->
                val quantity = bag[product.productId] ?: 0
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(cardPosterRes(product.cardId)),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(11.dp))
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("$quantity × ${product.title}", color = Color.Black.copy(alpha = 0.66f), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text(
                        billing.displayPrice(product.productId, product.fallbackPrice),
                        color = Color.Black,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Google Play confirms each digital item separately. Credits are granted only after server verification.",
            color = Color.Black.copy(alpha = 0.52f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (bagItems.isNotEmpty() && !productsAvailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                "One or more products are not configured for this Play build yet.",
                color = Color(0xFFB45309),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onCheckout,
            enabled = bagItems.isNotEmpty() && productsAvailable && !checkoutInProgress,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (checkoutInProgress) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Verifying purchase…", fontWeight = FontWeight.Black)
            } else {
                Text("Check out bag", fontWeight = FontWeight.Black)
            }
        }
        message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color.Black.copy(alpha = 0.72f), fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StoreCreditRow(
    icon: ImageVector,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    price: String,
    available: Boolean,
    onBuy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = CircleShape, color = iconBackground, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Text(subtitle, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.58f))
        }
        Button(onClick = onBuy, enabled = available) {
            Text(price, fontWeight = FontWeight.Black)
        }
    }
}

private fun cardPalette(cardId: String): Pair<Color, Color> = when (cardId) {
    "inverted" -> Color(0xFFDA347D) to Color(0xFF201122)
    "lens" -> Color(0xFF40D5F2) to Color(0xFF5C35C9)
    "splitDiamond" -> Color(0xFFF4B5FF) to Color(0xFF5578ED)
    "swoosh" -> Color(0xFFFF4F9A) to Color(0xFFFAAC45)
    else -> Color(0xFFFF4FA3) to Color(0xFF45203A)
}

private fun cardPosterRes(cardId: String): Int = when (cardId) {
    "inverted" -> R.drawable.store_card_inverted_poster
    "lens" -> R.drawable.store_card_lens_poster
    "splitDiamond" -> R.drawable.store_card_split_diamond_poster
    "swoosh" -> R.drawable.store_card_swoosh_poster
    else -> R.drawable.store_card_inverted_poster
}

@Composable
private fun LegacyProfileStorePanel(
    purchasedRadius: Int,
    onPurchase: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val billing = remember { PoplurBillingManager.getInstance(context) }
    val productDetails by billing.productDetails.collectAsState()
    val billingMessage by billing.message.collectAsState()
    val purchasedDelta by billing.purchasedRadiusDelta.collectAsState()
    val purchasedCredits by billing.creditsDelta.collectAsState()
    var deliveredDelta by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { billing.connect() }
    LaunchedEffect(purchasedDelta) {
        if (purchasedDelta > deliveredDelta) {
            onPurchase(purchasedDelta - deliveredDelta)
            deliveredDelta = purchasedDelta
        }
    }

    var selectedCardId by remember { mutableStateOf(PoplurCreditCardCatalog.first().id) }
    var orderMessage by remember { mutableStateOf<String?>(null) }
    val selectedCard = PoplurCreditCardCatalog.firstOrNull { it.id == selectedCardId } ?: PoplurCreditCardCatalog.first()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Poplur Store", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.Black)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Shop World Service cards, collectible finishes, and radius boosts.",
                    color = Color.Black.copy(alpha = 0.58f),
                    fontWeight = FontWeight.SemiBold
                )
            }
            Surface(shape = CircleShape, color = Color.Black, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text("P", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        Spacer(Modifier.height(22.dp))

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.045f))
                .padding(16.dp)
        ) {
            AnimatedPoplurCard(
                product = selectedCard,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(18.dp, RoundedCornerShape(18.dp), clip = false)
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(selectedCard.name, fontSize = 23.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Poplur World Service members card in ${selectedCard.material}.",
                        color = Color.Black.copy(alpha = 0.62f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(selectedCard.price, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StoreBadge("Contactless")
                StoreBadge("Numbered")
                StoreBadge(selectedCard.delivery)
            }
        }

        Spacer(Modifier.height(18.dp))

        Text("Choose a finish", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            PoplurCreditCardCatalog.forEach { product ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedCardId = product.id }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(product.accent)
                            .border(
                                width = if (selectedCardId == product.id) 3.dp else 0.dp,
                                color = if (selectedCardId == product.id) Color.Black else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedCardId == product.id) {
                            Text("✓", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(product.name.substringBefore(" "), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        PoplurCreditCardCatalog.chunked(2).forEach { rowProducts ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowProducts.forEach { product ->
                    PoplurCreditCardProductTile(
                        product = product,
                        selected = selectedCardId == product.id,
                        onClick = { selectedCardId = product.id },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowProducts.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.045f))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Order summary", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text("1 Poplur card - ${selectedCard.name}", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
                }
                Text(selectedCard.price, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.Black.copy(alpha = 0.12f))
            Spacer(Modifier.height(12.dp))
            Row {
                Text("Estimated shipping", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Free", color = Color(0xFF178A3D), fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = { orderMessage = "${selectedCard.name} card is ready for checkout." },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Buy ${selectedCard.name}", fontWeight = FontWeight.Black)
            }
            orderMessage?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = selectedCard.accent, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(22.dp))
        Text("Add radius", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(6.dp))
        Text("Boost your Poplur reach after choosing a card.", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
        if (purchasedRadius > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Purchased radius: ${purchasedRadius / 1000} km", color = Color(0xFF178A3D), fontWeight = FontWeight.Black)
        }
        billingMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = Color(0xFF178A3D), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        PoplurProducts.radiusPackages.forEach { pkg ->
            val available = productDetails.containsKey(pkg.productId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = Color(0xFFE9F8EE), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AttachMoney, null, tint = Color(0xFF178A3D), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(pkg.title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text("+${pkg.radiusMeters / 1000} km", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.58f))
                }
                Button(
                    onClick = {
                        (context as? Activity)?.let { billing.launchPurchase(it, pkg.productId) }
                    },
                    enabled = available
                ) {
                    Text(billing.displayPrice(pkg.productId, pkg.fallbackPrice), fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(22.dp))
        Text("Creator Credits", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(6.dp))
        Text("For AI images, video, and voice generation.", color = Color.Black.copy(alpha = 0.58f), fontWeight = FontWeight.SemiBold)
        if (purchasedCredits > 0) {
            Spacer(Modifier.height(10.dp))
            Text("Purchased credits: $purchasedCredits", color = Color(0xFFE0248C), fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(14.dp))
        PoplurProducts.creditPackages.forEach { pkg ->
            val available = productDetails.containsKey(pkg.productId)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.05f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = Color(0xFFFCE3F0), modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Bolt, null, tint = Color(0xFFE0248C), modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(pkg.title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text("${pkg.credits} credits", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black.copy(alpha = 0.58f))
                }
                Button(
                    onClick = {
                        (context as? Activity)?.let { billing.launchPurchase(it, pkg.productId) }
                    },
                    enabled = available
                ) {
                    Text(billing.displayPrice(pkg.productId, pkg.fallbackPrice), fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun AnimatedPoplurCard(
    product: PoplurCreditCardProduct,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(product.frameResIds.first()),
        contentDescription = product.name,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .aspectRatio(482f / 320f)
            .clip(RoundedCornerShape(18.dp))
    )
}

@Composable
private fun PoplurCreditCardProductTile(
    product: PoplurCreditCardProduct,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = if (selected) 0.075f else 0.035f))
            .border(2.dp, if (selected) product.accent else Color.Transparent, shape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        AnimatedPoplurCard(product = product, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(product.name, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.Black)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(product.price, fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.Black)
            Spacer(Modifier.weight(1f))
            Text(if (selected) "✓" else "+", color = if (selected) product.accent else Color.Black.copy(alpha = 0.72f), fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StoreBadge(text: String) {
    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.88f)) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
fun ProfileTabItem(
    text: String,
    isSelected: Boolean,
    cubeSkin: CubeSkin = CubeSkin.Default,
    contentColor: Color = Color.Black,
    accentColor: Color? = null,
    onClick: () -> Unit
) {
    val theme = cubeSkin.uiTheme()
    Column(modifier = Modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) contentColor else contentColor.copy(alpha = 0.72f)
        )
        if (isSelected) Spacer(Modifier.height(4.dp))
        if (isSelected) Box(
            Modifier
                .width(48.dp)
                .height(3.dp)
                .background(accentColor ?: theme.accent, RoundedCornerShape(2.dp))
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun MediaItem(url: String? = null, radius: Int = 5000, cubeSkin: CubeSkin = CubeSkin.Default, isVideo: Boolean = false) {
    val shape = RoundedCornerShape(18.dp)

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(shape)
            .background(Color.Gray.copy(alpha = 0.26f))
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else if (isVideo) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.84f),
                    modifier = Modifier.size(46.dp)
                )
            }
        } else {
            cubeSkin.assetPath?.let { assetPath ->
                AsyncImage(
                    model = "file:///android_asset/$assetPath",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f)
                        )
                    )
                )
        )
        Surface(modifier = Modifier.padding(8.dp).align(Alignment.BottomStart), color = Color.Black.copy(alpha = 0.58f), shape = RoundedCornerShape(12.dp)) {
            Text(
                text = "${(radius / 1000).coerceAtLeast(1)} KM",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        ProfileScreenContent(profile = UserProfile())
    }
}

package com.apptists.poplur2025.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import coil.compose.AsyncImage
import com.apptists.poplur2025.billing.PoplurBillingManager
import com.apptists.poplur2025.billing.PoplurProducts

import com.apptists.poplur2025.R
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.BusinessVerificationStatus
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.ui.CubeSkin
import com.apptists.poplur2025.ui.CubeSkinBackgroundLayer
import com.apptists.poplur2025.ui.PoplurAIProfileSortOrder
import com.apptists.poplur2025.ui.components.ProfileAvatarSpeaker
import com.apptists.poplur2025.ui.components.RadiusDisplay
import com.apptists.poplur2025.ui.components.TrendingDisplay
import com.apptists.poplur2025.ui.components.poplurCountdownDisplayModifier
import com.apptists.poplur2025.ui.components.poplurRadiusDisplayModifier
import com.apptists.poplur2025.ui.uiTheme
import kotlinx.coroutines.delay
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
    assistantSortOrder: PoplurAIProfileSortOrder? = null,
    assistantSortToken: Int = 0,
    showHeyPoplurTutorial: Boolean = false,
    onHeyPoplurTutorialShown: () -> Unit = {},
    onTutorialPulseRequest: () -> Unit = {}
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
        assistantSortOrder = assistantSortOrder,
        assistantSortToken = assistantSortToken,
        showHeyPoplurTutorial = showHeyPoplurTutorial,
        onHeyPoplurTutorialShown = onHeyPoplurTutorialShown,
        onTutorialPulseRequest = onTutorialPulseRequest
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
    assistantSortOrder: PoplurAIProfileSortOrder? = null,
    assistantSortToken: Int = 0,
    showHeyPoplurTutorial: Boolean = false,
    onHeyPoplurTutorialShown: () -> Unit = {},
    onTutorialPulseRequest: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var currentProfile by remember { mutableStateOf(profile) }
    var isEditing by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf(ProfilePanel.Home) }
    var hasUncheckedNotifications by remember { mutableStateOf(true) }
    var purchasedRadius by remember { mutableIntStateOf(0) }
    var radiusSortOrder by remember { mutableStateOf<PoplurAIProfileSortOrder?>(null) }

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

    LaunchedEffect(assistantSortToken) {
        if (assistantSortToken > 0) {
            radiusSortOrder = assistantSortOrder ?: PoplurAIProfileSortOrder.MostRadius
            selectedTab = 0
            activePanel = ProfilePanel.Home
        }
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
            radiusSortOrder = radiusSortOrder,
            showHeyPoplurTutorial = showHeyPoplurTutorial,
            onHeyPoplurTutorialShown = onHeyPoplurTutorialShown,
            onTutorialPulseRequest = onTutorialPulseRequest
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
    radiusSortOrder: PoplurAIProfileSortOrder? = null,
    showHeyPoplurTutorial: Boolean = false,
    onHeyPoplurTutorialShown: () -> Unit = {},
    onTutorialPulseRequest: () -> Unit = {}
) {
    val theme = cubeSkin.uiTheme()
    var didStartHeyPoplurTutorial by rememberSaveable { mutableStateOf(false) }
    var showNotificationBanner by remember { mutableStateOf(false) }
    var localUnread by remember { mutableStateOf(hasUncheckedNotifications) }
    val mediaPosts = when (radiusSortOrder) {
        PoplurAIProfileSortOrder.MostRadius -> DemoPostStore.posts.sortedByDescending { it.radius }
        PoplurAIProfileSortOrder.LeastRadius -> DemoPostStore.posts.sortedBy { it.radius }
        null -> DemoPostStore.posts
    }
    val profilePostCount = mediaPosts.size
    val profileTotalRadius = mediaPosts.sumOf { it.radius.coerceAtLeast(0) }

    LaunchedEffect(showHeyPoplurTutorial) {
        if (showHeyPoplurTutorial && !didStartHeyPoplurTutorial) {
            didStartHeyPoplurTutorial = true
            delay(1_200)
            onTutorialPulseRequest()
            delay(5_800)
            onHeyPoplurTutorialShown()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
    ) {
        CubeSkinBackgroundLayer(
            skin = cubeSkin,
            opacity = 0.16f,
            baseColor = Color.White,
            modifier = Modifier.matchParentSize()
        )

        Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            ProfileAvatarSpeaker(
                avatarUrl = profile.avatarUrl,
                totalRadiusMeters = profileTotalRadius,
                postCount = profilePostCount,
                size = if (avatarIs3D) 180.dp else 96.dp,
                is3D = avatarIs3D,
                accentColor = theme.accent,
                cubeSkin = cubeSkin,
                onWakeActivated = onTutorialPulseRequest
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = profile.profileDisplayName.uppercase(),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = Color.Black.copy(alpha = 0.56f),
                    modifier = Modifier.size(20.dp).clickable { onEdit() }
                )
            }
            BusinessProfileBadge(profile = profile)
            Row(Modifier.fillMaxWidth().padding(top = 22.dp), horizontalArrangement = Arrangement.Center) {
                ProfileTabItem("Media", selectedTab == 0, cubeSkin = cubeSkin) { onTab(0) }
                Spacer(Modifier.width(56.dp))
                ProfileTabItem("Messages", isSelected = false, cubeSkin = cubeSkin, onClick = onOpenMessages)
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
            contentColor = theme.accent,
            modifier = poplurCountdownDisplayModifier()
        )

        RadiusDisplay(
            modifier = poplurRadiusDisplayModifier(),
            radius = 56,
            unit = "mi",
            contentColor = theme.accent
        )

        if (showHeyPoplurTutorial) {
            HeyPoplurProfileTutorialCard(
                accent = theme.accent,
                onDismiss = onHeyPoplurTutorialShown,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, bottom = 146.dp)
            )
        }

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
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showNotificationBanner = false }
            )
        }

        AnimatedVisibility(
            visible = showNotificationBanner,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
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
private fun BusinessProfileBadge(profile: UserProfile) {
    val business = profile.businessProfile ?: return
    val color = businessStatusColor(business.verificationStatus)
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = if (business.verificationStatus == BusinessVerificationStatus.Verified) {
                "Verified Business"
            } else {
                business.verificationStatus.label
            },
            color = Color.Black,
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
                color = Color.Black.copy(alpha = 0.58f),
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
private fun HeyPoplurProfileTutorialCard(
    accent: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = Color.Black.copy(alpha = 0.78f),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .border(1.5.dp, accent.copy(alpha = 0.9f), RoundedCornerShape(22.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Say \"Hey Poplur\"",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "The edges light up in your theme color. Then tell Poplur what to do.",
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = "Got it",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }
    }
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
fun ProfileTabItem(text: String, isSelected: Boolean, cubeSkin: CubeSkin = CubeSkin.Default, onClick: () -> Unit) {
    val theme = cubeSkin.uiTheme()
    Column(modifier = Modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) Color.Black else Color.Black.copy(alpha = 0.72f)
        )
        if (isSelected) Spacer(Modifier.height(4.dp))
        if (isSelected) Box(Modifier.width(48.dp).height(3.dp).background(theme.accent, RoundedCornerShape(2.dp)))
    }
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

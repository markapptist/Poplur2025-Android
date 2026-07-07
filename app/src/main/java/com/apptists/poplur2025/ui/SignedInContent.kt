package com.apptists.poplur2025.ui

import android.net.Uri
import android.graphics.Bitmap
import android.content.SharedPreferences
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.apptists.poplur2025.auth.AuthViewModel
import com.apptists.poplur2025.data.AIVideoService
import com.apptists.poplur2025.data.AIVideoStyle
import com.apptists.poplur2025.data.DemoPostStore
import com.apptists.poplur2025.data.TVStyleMetadata
import com.apptists.poplur2025.data.UserProfile
import com.apptists.poplur2025.data.AppTab
import com.apptists.poplur2025.data.PoplurLanguage
import com.apptists.poplur2025.ui.screens.*
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*

@Composable
fun SignedInContent(
    tab: AppTab,
    currentPostIndex: Int,
    isAppVisible: Boolean,
    feedTvGuideRequest: Int,
    feedTargetChannelQuery: String,
    feedTargetChannelToken: Int,
    feedNewsRequest: Int,
    feedLikeRequest: Int,
    feedOpenProfileRequest: Int,
    feedCommentSubmitRequest: Int,
    submittedFeedComment: String,
    showFeedCommentComposer: Boolean,
    preferredLanguage: PoplurLanguage,
    closedCaptionsEnabled: Boolean,
    isFeedChromeAsleep: Boolean,
    selectedProfile: UserProfile,
    currentUserProfile: UserProfile,
    profileCubeCommand: ProfileCubeMenuCommand?,
    profileAvatarIs3D: Boolean,
    selectedCubeSkin: CubeSkin,
    profileSortOrder: PoplurAIProfileSortOrder?,
    profileSortToken: Int,
    hasSeenProfileWakeTutorial: Boolean,
    profileWakeTutorialPulse: Int,
    capturedMediaUri: Uri?,
    capturePostStep: CapturePostStep,
    isCapturedVideo: Boolean,
    selectedCaptureFilterId: String,
    selectedCaptureTrimSeconds: Float,
    selectedCaptureMusicUri: Uri?,
    postCaptureCreationMode: PostCaptureCreationMode,
    capturedAdBitmap: Bitmap?,
    capturedAdVideoUrl: String?,
    capturedAdMeta: TVStyleMetadata?,
    pendingCaptureLocation: CaptureMapPoint?,
    captureFlowOrigin: CaptureFlowOrigin,
    cameraPhotoTrigger: Int,
    cameraRecordTrigger: Int,
    cameraLiveTrigger: Int,
    cameraFiltersTrigger: Int,
    cameraObjectDetectionEnabled: Boolean,
    aiVideoPrompt: String,
    aiVideoStyle: AIVideoStyle,
    aiVideoLocationName: String,
    previousTab: AppTab?,
    authViewModel: AuthViewModel,
    mapViewModel: MapViewModel,
    scope: CoroutineScope,
    settingsPrefs: SharedPreferences,
    playPopSound: () -> Unit,
    onTabChange: (AppTab) -> Unit,
    onPreviousTabChange: (AppTab?) -> Unit,
    onCurrentPostIndexChange: (Int) -> Unit,
    onFeedTvGuideRequestChange: (Int) -> Unit,
    onFeedTargetChannelQueryChange: (String) -> Unit,
    onFeedTargetChannelTokenChange: (Int) -> Unit,
    onFeedNewsRequestChange: (Int) -> Unit,
    onFeedLikeRequestChange: (Int) -> Unit,
    onFeedOpenProfileRequestChange: (Int) -> Unit,
    onFeedCommentSubmitRequestChange: (Int) -> Unit,
    onSubmittedFeedCommentChange: (String) -> Unit,
    onShowFeedCommentComposerChange: (Boolean) -> Unit,
    onSelectedProfileChange: (UserProfile) -> Unit,
    onProfileCubeCommittedIconTitleChange: (String?) -> Unit,
    onProfilePanelIconTitleChange: (String?) -> Unit,
    onPhoneColorwaySelectedChange: (androidx.compose.ui.graphics.Color) -> Unit,
    onIsPhoneActiveChange: (Boolean) -> Unit,
    onIsProfileSubActiveChange: (Boolean) -> Unit,
    onIsPhoneCallScreenVisibleChange: (Boolean) -> Unit,
    onHasSeenProfileWakeTutorialChange: (Boolean) -> Unit,
    onProfileWakeTutorialPulseChange: (Int) -> Unit,
    onCapturedMediaUriChange: (Uri?) -> Unit,
    onCapturePostStepChange: (CapturePostStep) -> Unit,
    onIsCapturedVideoChange: (Boolean) -> Unit,
    onSelectedCaptureFilterIdChange: (String) -> Unit,
    onSelectedCaptureTrimSecondsChange: (Float) -> Unit,
    onSelectedCaptureMusicUriChange: (Uri?) -> Unit,
    onPostCaptureCreationModeChange: (PostCaptureCreationMode) -> Unit,
    onCapturedAdBitmapChange: (Bitmap?) -> Unit,
    onCapturedAdVideoUrlChange: (String?) -> Unit,
    onCapturedAdMetaChange: (TVStyleMetadata?) -> Unit,
    onPendingCaptureLocationChange: (CaptureMapPoint?) -> Unit,
    onCaptureFlowOriginChange: (CaptureFlowOrigin) -> Unit,
    onCameraPhotoTriggerChange: (Int) -> Unit,
    onCameraFiltersToggle: (Boolean) -> Unit,
    onChannelObjectPop: (String) -> Unit,
    onAiVideoPromptChange: (String) -> Unit,
    onAiVideoStyleChange: (AIVideoStyle) -> Unit,
    onAiVideoLocationNameChange: (String) -> Unit,
    onIsFeedTvGuideVisibleChange: (Boolean) -> Unit,
    onPhoneColorwaySelected: androidx.compose.ui.graphics.Color?
) {
    val haptics = LocalHapticFeedback.current
    
    when (tab) {
        AppTab.Feed -> FeedScreen(
            currentPostIndex = currentPostIndex,
            isVisible = isAppVisible,
            tvGuideRequestToken = feedTvGuideRequest,
            targetChannelQuery = feedTargetChannelQuery,
            targetChannelToken = feedTargetChannelToken,
            timelineToggleRequestToken = feedNewsRequest,
            likeRequestToken = feedLikeRequest,
            openProfileRequestToken = feedOpenProfileRequest,
            submittedComment = submittedFeedComment,
            commentSubmitToken = feedCommentSubmitRequest,
            preferredLanguage = preferredLanguage,
            closedCaptionsEnabled = closedCaptionsEnabled,
            homeChromeInactive = isFeedChromeAsleep || showFeedCommentComposer,
            cubeSkin = selectedCubeSkin,
            onOpenUser = { profile ->
                onSelectedProfileChange(profile)
                onPreviousTabChange(tab)
                onTabChange(AppTab.Profile)
            },
            onTvGuideVisibilityChanged = { onIsFeedTvGuideVisibleChange(it) },
            onChannelObjectPop = { objectId ->
                onChannelObjectPop(objectId)
            },
            onUserInteraction = { /* ... */ },
            onIndexChange = {
                onCurrentPostIndexChange(it)
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                playPopSound()
            }
        )
        AppTab.Profile -> ProfileScreen(
            profile = selectedProfile,
            authViewModel = authViewModel,
            onOpenSettings = {
                onProfileCubeCommittedIconTitleChange("Settings")
                onPreviousTabChange(AppTab.Profile)
                onTabChange(AppTab.Settings)
            },
            onOpenMessages = {
                onProfileCubeCommittedIconTitleChange("Mail")
                onPreviousTabChange(AppTab.Profile)
                onTabChange(AppTab.Messages)
            },
            cubeMenuCommand = profileCubeCommand,
            onProfilePanelIconChanged = { iconTitle ->
                onProfilePanelIconTitleChange(iconTitle)
                onProfileCubeCommittedIconTitleChange(iconTitle)
                onIsPhoneActiveChange(iconTitle == "Phone")
                onIsProfileSubActiveChange(iconTitle != null)
            },
            onPhoneCallVisibilityChanged = { onIsPhoneCallScreenVisibleChange(it) },
            onColorwaySelected = { color -> onPhoneColorwaySelectedChange(color) },
            forcedColorway = onPhoneColorwaySelected,
            avatarIs3D = profileAvatarIs3D,
            cubeSkin = selectedCubeSkin,
            assistantSortOrder = profileSortOrder,
            assistantSortToken = profileSortToken,
            showHeyPoplurTutorial = !hasSeenProfileWakeTutorial,
            onHeyPoplurTutorialShown = {
                if (!hasSeenProfileWakeTutorial) {
                    onHasSeenProfileWakeTutorialChange(true)
                    settingsPrefs.edit().putBoolean("profile_hey_poplur_tutorial_seen", true).apply()
                }
            },
            onTutorialPulseRequest = {
                onProfileWakeTutorialPulseChange(profileWakeTutorialPulse + 1)
            }
        )
        AppTab.Map -> MapScreen(
            isVisible = isAppVisible,
            mapViewModel = mapViewModel,
            isSelectingPostLocation = capturedMediaUri != null && capturePostStep == CapturePostStep.Location,
            cubeSkin = selectedCubeSkin,
            onCaptureLocationConfirmed = { point ->
                onPendingCaptureLocationChange(CaptureMapPoint(point.latitude(), point.longitude()))
                onCapturePostStepChange(CapturePostStep.Edit)
                onTabChange(AppTab.CapturedOutput)
            },
            onMapFirstPostLocationSelected = { point ->
                onPendingCaptureLocationChange(CaptureMapPoint(point.latitude(), point.longitude()))
                onCaptureFlowOriginChange(CaptureFlowOrigin.MapFirst)
                onSelectedCaptureFilterIdChange("original")
                onSelectedCaptureTrimSecondsChange(10f)
                onCapturedMediaUriChange(null)
                onPreviousTabChange(AppTab.Map)
                onTabChange(AppTab.Camera)
            },
            onChannelObjectPop = onChannelObjectPop
        )
        AppTab.CreateEntry -> com.apptists.poplur2025.ui.screens.CreateEntryScreen(
            onOriginalContent = {
                onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                onCapturePostStepChange(CapturePostStep.Preview)
                onPendingCaptureLocationChange(null)
                onCaptureFlowOriginChange(CaptureFlowOrigin.CameraFirst)
                onTabChange(AppTab.Camera)
            },
            onPoplurAI = {
                onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                onCapturePostStepChange(CapturePostStep.Location)
                onPendingCaptureLocationChange(null)
                onCapturedMediaUriChange(null)
                onCaptureFlowOriginChange(CaptureFlowOrigin.MapFirst)
                onPreviousTabChange(AppTab.CreateEntry)
                onTabChange(AppTab.Map)
                scope.launch { mapViewModel.triggerPostToMap() }
            },
            onBack = { onTabChange(AppTab.Camera) }
        )
        AppTab.AIVideoStudio -> com.apptists.poplur2025.ui.screens.AIVideoStudioScreen(
            initialPrompt = aiVideoPrompt,
            initialStyle = aiVideoStyle,
            initialLocationName = aiVideoLocationName,
            onBack = { onTabChange(previousTab ?: AppTab.Camera) },
            onGenerate = { prompt, style, locationName ->
                onAiVideoPromptChange(prompt)
                onAiVideoStyleChange(style)
                onAiVideoLocationNameChange(locationName)
                onPreviousTabChange(AppTab.AIVideoStudio)
                onTabChange(AppTab.AIVideoProgress)
                scope.launch { AIVideoService.generate(prompt, style, locationName) }
            }
        )
        AppTab.AIVideoProgress -> com.apptists.poplur2025.ui.screens.AIVideoProgressScreen(
            onBack = { onTabChange(previousTab ?: AppTab.AIVideoStudio) },
            onPostClip = { uri, isVideo ->
                onCapturedMediaUriChange(uri)
                onIsCapturedVideoChange(isVideo)
                onCaptureFlowOriginChange(CaptureFlowOrigin.CameraFirst)
                onCapturePostStepChange(CapturePostStep.Channel)
                onPostCaptureCreationModeChange(PostCaptureCreationMode.PoplurAI)
                onSelectedCaptureFilterIdChange("original")
                onSelectedCaptureTrimSecondsChange(0f)
                onSelectedCaptureMusicUriChange(null)
                onPreviousTabChange(AppTab.AIVideoProgress)
                onTabChange(AppTab.CapturedOutput)
            }
        )
        AppTab.Camera -> CameraScreen(
            photoTrigger = cameraPhotoTrigger,
            recordTrigger = cameraRecordTrigger,
            liveTrigger = cameraLiveTrigger,
            filtersTrigger = cameraFiltersTrigger,
            objectDetectionEnabled = cameraObjectDetectionEnabled,
            currentUserProfile = currentUserProfile,
            cubeSkin = selectedCubeSkin,
            onFiltersToggle = { onCameraFiltersToggle(it) },
            onObjectPop = onChannelObjectPop,
            onMediaCaptured = { uri, isVideo ->
                onCapturedMediaUriChange(uri)
                onIsCapturedVideoChange(isVideo)
                if (pendingCaptureLocation == null) {
                    onCaptureFlowOriginChange(CaptureFlowOrigin.CameraFirst)
                }
                onCapturePostStepChange(CapturePostStep.Preview)
                onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                onSelectedCaptureFilterIdChange("original")
                onSelectedCaptureTrimSecondsChange(10f)
                onSelectedCaptureMusicUriChange(null)
                onCapturedAdBitmapChange(null)
                onCapturedAdVideoUrlChange(null)
                onCapturedAdMetaChange(null)
                onTabChange(AppTab.CapturedOutput)
            }
        )
        AppTab.CapturedOutput -> {
            val uri = capturedMediaUri
            if (uri != null) {
                when (capturePostStep) {
                    CapturePostStep.CreateChoice -> CapturedOutputScreen(
                        uri = uri, isVideo = isCapturedVideo, filterId = selectedCaptureFilterId,
                        onBack = { onCapturePostStepChange(CapturePostStep.Edit) },
                        showCreationOptions = true,
                        onUseOriginal = {
                            onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                            onCapturedAdBitmapChange(null)
                            onCapturedAdVideoUrlChange(null)
                            onCapturedAdMetaChange(null)
                            onCapturePostStepChange(CapturePostStep.Channel)
                        },
                        onUsePoplurAI = {
                            onPostCaptureCreationModeChange(PostCaptureCreationMode.PoplurAI)
                            onCapturePostStepChange(CapturePostStep.AdDescribe)
                        }
                    )
                    CapturePostStep.Preview -> CapturedOutputScreen(
                        uri = uri, isVideo = isCapturedVideo,
                        onBack = {
                            onCapturedMediaUriChange(null)
                            onPendingCaptureLocationChange(null)
                            onCaptureFlowOriginChange(CaptureFlowOrigin.CameraFirst)
                            onCapturePostStepChange(CapturePostStep.Preview)
                            onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                            onSelectedCaptureMusicUriChange(null)
                            onTabChange(AppTab.Camera)
                        },
                        continueLabel = "Keep",
                        onContinue = {
                            if (pendingCaptureLocation == null) {
                                onCapturePostStepChange(CapturePostStep.Location)
                                onTabChange(AppTab.Map)
                            } else {
                                onCapturePostStepChange(CapturePostStep.Edit)
                            }
                        }
                    )
                    CapturePostStep.Location -> { onTabChange(AppTab.Map) }
                    CapturePostStep.Edit -> CapturedMediaEditorScreen(
                        uri = uri, isVideo = isCapturedVideo,
                        selectedFilterId = selectedCaptureFilterId, trimSeconds = selectedCaptureTrimSeconds,
                        selectedMusicUri = selectedCaptureMusicUri,
                        onFilterSelected = { onSelectedCaptureFilterIdChange(it) },
                        onTrimSecondsChanged = { onSelectedCaptureTrimSecondsChange(it) },
                        onMusicSelected = { onSelectedCaptureMusicUriChange(it) },
                        onBack = { onCapturePostStepChange(CapturePostStep.Preview) },
                        onNext = { onCapturePostStepChange(CapturePostStep.CreateChoice) }
                    )
                    CapturePostStep.Channel -> CapturedChannelScreen(
                        uri = uri, isVideo = isCapturedVideo, filterId = selectedCaptureFilterId,
                        trimSeconds = if (isCapturedVideo) selectedCaptureTrimSeconds else null,
                        knownMetadata = if (postCaptureCreationMode == PostCaptureCreationMode.PoplurAI) capturedAdMeta else null,
                        musicUri = if (postCaptureCreationMode == PostCaptureCreationMode.Original) selectedCaptureMusicUri else null,
                        isPoplurAIOutput = postCaptureCreationMode == PostCaptureCreationMode.PoplurAI,
                        onBack = {
                            if (postCaptureCreationMode == PostCaptureCreationMode.PoplurAI && (capturedAdBitmap != null || capturedAdVideoUrl != null)) {
                                onCapturePostStepChange(CapturePostStep.AdPreview)
                            } else {
                                onCapturePostStepChange(CapturePostStep.CreateChoice)
                            }
                        },
                        onPosted = { channel, meta, outputUri, outputIsVideo ->
                            val point = pendingCaptureLocation ?: CaptureMapPoint(0.0, 0.0)
                            DemoPostStore.addUserPost(
                                uri = outputUri.toString(), isVideo = outputIsVideo,
                                latitude = point.latitude, longitude = point.longitude,
                                channelId = channel.id, channelTitle = channel.title,
                                filterId = selectedCaptureFilterId,
                                trimSeconds = if (outputIsVideo && postCaptureCreationMode == PostCaptureCreationMode.Original) selectedCaptureTrimSeconds else null,
                                tvMetadata = meta
                            )
                            onCapturedMediaUriChange(null); onCapturedAdBitmapChange(null); onCapturedAdVideoUrlChange(null)
                            onCapturedAdMetaChange(null); onPendingCaptureLocationChange(null); onCaptureFlowOriginChange(CaptureFlowOrigin.CameraFirst)
                            onCapturePostStepChange(CapturePostStep.Preview); onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                            onSelectedCaptureFilterIdChange("original"); onSelectedCaptureTrimSecondsChange(10f); onSelectedCaptureMusicUriChange(null)
                            onTabChange(AppTab.Profile)
                        }
                    )
                    CapturePostStep.AdDescribe -> {
                        com.apptists.poplur2025.ui.screens.DescribeAdScreen(
                            uri = uri, isVideo = isCapturedVideo,
                            coordinate = pendingCaptureLocation?.let { Pair(it.latitude, it.longitude) },
                            onBack = { onCapturePostStepChange(CapturePostStep.CreateChoice) },
                            onAdGenerated = { b, v, m -> onCapturedAdBitmapChange(b); onCapturedAdVideoUrlChange(v); onCapturedAdMetaChange(m); onCapturePostStepChange(CapturePostStep.AdPreview) }
                        )
                    }
                    CapturePostStep.AdPreview -> {
                        val bitmap = capturedAdBitmap; val videoUrl = capturedAdVideoUrl; val meta = capturedAdMeta
                        if ((bitmap != null || videoUrl != null) && meta != null) {
                            com.apptists.poplur2025.ui.screens.AdPreviewScreen(
                                adBitmap = bitmap, adVideoUrl = videoUrl,
                                selectedChannel = com.apptists.poplur2025.ui.screens.postCaptureChannels.first(),
                                meta = meta, onBack = { onCapturePostStepChange(CapturePostStep.AdDescribe) },
                                onBroadcast = { outputUri, outputIsVideo ->
                                    onCapturedMediaUriChange(outputUri); onIsCapturedVideoChange(outputIsVideo)
                                    onSelectedCaptureFilterIdChange("original"); onSelectedCaptureTrimSecondsChange(10f); onSelectedCaptureMusicUriChange(null)
                                    onCapturePostStepChange(CapturePostStep.Channel)
                                }
                            )
                        } else { onCapturePostStepChange(CapturePostStep.AdDescribe) }
                    }
                }
            } else {
                onCapturePostStepChange(CapturePostStep.Preview)
                onPostCaptureCreationModeChange(PostCaptureCreationMode.Original)
                onTabChange(AppTab.Camera)
            }
        }
        AppTab.Home -> HomeScreen(onOpenCamera = { onTabChange(AppTab.Camera) }, onOpenMap = { onTabChange(AppTab.Map) })
        AppTab.Messages -> DMScreen(
            onBack = {
                val dest = previousTab ?: AppTab.Feed
                onTabChange(dest)
                if (dest == AppTab.Profile) { onProfileCubeCommittedIconTitleChange(null); onProfilePanelIconTitleChange(null) }
            },
            onPhoneCallVisibilityChanged = { onIsPhoneCallScreenVisibleChange(it) }
        )
        AppTab.Settings -> SettingsScreen(
            onBack = {
                onTabChange(previousTab ?: AppTab.Profile)
                if (tab == AppTab.Profile) { onProfileCubeCommittedIconTitleChange(null); onProfilePanelIconTitleChange(null) }
            }
        )
        AppTab.CubeDebug -> com.apptists.poplur2025.ui.previews.CubeDebugScreen(
            onBack = { onTabChange(previousTab ?: AppTab.Profile) }
        )
        else -> Unit
    }
}

package com.apptists.poplur2025.call

import android.content.Context
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

// Replace with your Agora App ID from console.agora.io
private const val AGORA_APP_ID = "YOUR_AGORA_APP_ID"

object AgoraCallEngine {

    private var engine: RtcEngine? = null

    private val eventHandler = object : IRtcEngineEventHandler() {
        override fun onUserOffline(uid: Int, reason: Int) {
            // Remote user left — end the call if we are currently connected
            if (CallSessionManager.callState.value is CallSessionManager.CallState.Connected) {
                CallSessionManager.endCall()
            }
        }
        override fun onError(err: Int) {
            android.util.Log.e("AgoraCallEngine", "RtcEngine error $err")
        }
    }

    fun initialize(context: Context) {
        if (engine != null) return
        try {
            val config = RtcEngineConfig().apply {
                mContext = context.applicationContext
                mAppId = AGORA_APP_ID
                mEventHandler = eventHandler
            }
            engine = RtcEngine.create(config)
            engine?.apply {
                enableAudio()
                disableVideo()
                setDefaultAudioRoutetoSpeakerphone(false) // earpiece by default, like a phone call
            }
        } catch (e: Exception) {
            android.util.Log.e("AgoraCallEngine", "Failed to initialize Agora RtcEngine: ${e.message}")
        }
    }

    fun joinChannel(channelId: String) {
        val e = engine ?: return
        val options = ChannelMediaOptions().apply {
            channelProfile    = Constants.CHANNEL_PROFILE_COMMUNICATION
            clientRoleType    = Constants.CLIENT_ROLE_BROADCASTER
            autoSubscribeAudio = true
            publishMicrophoneTrack = true
            publishCameraTrack     = false
        }
        e.joinChannel(null, channelId, 0, options)
    }

    fun leaveChannel() {
        engine?.leaveChannel()
    }

    fun release() {
        engine?.let { RtcEngine.destroy() }
        engine = null
    }
}

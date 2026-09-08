package com.apptists.poplur2025

import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.apptists.poplur2025.prelogin.*
import com.apptists.poplur2025.ui.components.PreLoginMusicPlayer
import com.apptists.poplur2025.ui.components.PoplurVolumeBars
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The entire signed-out activity is native Compose/Filament. It never creates
 * a WebView, and only a completed auth/profile operation can launch Home. */
class NativePreloginActivity : ComponentActivity() {
    private val model: NativePreloginModel by viewModels()
    private lateinit var music: PreLoginMusicPlayer
    private lateinit var biometric: NativePreloginBiometrics
    private lateinit var audio: AudioManager
    private var foreground by mutableStateOf(false)
    private var volumeVisible by mutableStateOf(false)
    private var fraction by mutableFloatStateOf(0f)
    private var volumeJob: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        music = PreLoginMusicPlayer(this); biometric = NativePreloginBiometrics(this)
        audio = getSystemService(AUDIO_SERVICE) as AudioManager
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = 0xFFFF2D96.toInt()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFFF2D96))) {
                Box(Modifier.fillMaxSize()) {
                    NativePreloginScreen(model, music, foreground, onSignIn = { enable ->
                        model.signIn(if (enable) { { biometric.save(model.email.trim(), model.password, model::finish) } } else null)
                    }, onBiometric = {
                        biometric.signIn({ email, password -> model.email = email; model.password = password; model.signIn() }, { model.error = it })
                    }, onExit = { moveTaskToBack(true) })
                    if (volumeVisible) PoplurVolumeBars(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp), volumeFraction = fraction)
                }
                LaunchedEffect(model.completed, foreground) {
                    if (model.completed && foreground) {
                        music.stop()
                        startActivity(Intent(this@NativePreloginActivity, PostLoginWebActivity::class.java).putExtra("fromAuth", true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                        finish()
                    }
                }
            }
        }
    }
    override fun onResume() { super.onResume(); foreground = true; music.start() }
    override fun onPause() { foreground = false; music.stop(); super.onPause() }
    override fun onDestroy() { biometric.cancel(); music.dispose(); volumeJob?.cancel(); super.onDestroy() }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP || event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER, 0)
                fraction = audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                volumeVisible = true; volumeJob?.cancel(); volumeJob = lifecycleScope.launch { delay(1600); volumeVisible = false }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

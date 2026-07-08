package com.apptists.poplur2025.ui.components

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.filament.IndirectLight
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import kotlin.math.sin

/**
 * The 3D "Mark head" for the Profile screen, rendered with SceneView (Google Filament).
 *
 * Filament runs in its own GL context, so — unlike the previous libGDX-based head — this does
 * NOT contend with the process-global `Gdx.*` singletons owned by the persistent cube overlay.
 * That contention is why the libGDX head never appeared on Android.
 *
 * Mirrors the iOS SceneKit head (MarkHeadView.swift): a gentle idle sway + bob that becomes
 * more animated while the avatar is speaking.
 */
@Composable
fun MarkHeadSceneView(
    isSpeaking: Boolean,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val speaking = rememberUpdatedState(isSpeaking)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            SceneView(ctx).apply {
                lifecycle = lifecycleOwner.lifecycle
                try {
                    // Flat ambient IBL so the PBR head isn't rendered black. The bare
                    // SceneView constructor sets up no environment, so without this the
                    // model shows up completely dark.
                    indirectLight = IndirectLight.Builder()
                        .irradiance(1, floatArrayOf(1.0f, 1.0f, 1.05f))
                        .intensity(55_000f)
                        .build(engine)

                    val instance = modelLoader.createModelInstance("mark_head/MarkHead.glb")
                    val head = ModelNode(
                        modelInstance = instance,
                        scaleToUnits = 1.6f,
                        centerOrigin = Float3(0f, 0f, 0f),
                    )
                    childNodes = listOf(head)
                    // The head must be framed explicitly: SceneView's default camera sits
                    // at the origin (inside this model, which is ~190 units natively before
                    // scaleToUnits), so without moving it back the view is empty/black.
                    cameraNode.position = Float3(0f, 0f, 3.5f)
                    cameraNode.lookAt(head)

                    var t = 0f
                    val basePos = head.position
                    onFrame = {
                        t += 1f / 60f
                        val amp = if (speaking.value) 1f else 0.3f
                        // Idle sway + subtle bob; livelier while speaking.
                        head.rotation = Float3(0f, sin(t * 0.7f) * 14f * amp, 0f)
                        head.position = Float3(basePos.x, basePos.y + sin(t * 2.4f) * 0.018f * amp, basePos.z)
                    }
                } catch (e: Exception) {
                    Log.e("MarkHeadSceneView", "Failed to load mark_head/MarkHead.glb", e)
                }
            }
        },
        onRelease = { it.destroy() }
    )
}

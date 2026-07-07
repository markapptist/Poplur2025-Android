package com.markgame.character;

import com.badlogic.gdx.graphics.g3d.utils.AnimationController;
import net.mgsx.gltf.scene3d.scene.Scene;

/**
 * Drives the character's animation clips on top of gdx-gltf's Scene, which owns a
 * standard libGDX AnimationController (scene.animationController) for playing a
 * single blended animation at a time — see gdx-gltf's docs: "Scene own a
 * AnimationController to play animation on its modelInstance... you typically use
 * scene.animationController to play single animation and actions."
 *
 * Clip names below match what's actually embedded in
 * Mark_NextGen_PlatformerHero_Red_Animated_Rigged.glb (verified directly
 * against the file, not assumed).
 *
 * IMPORTANT: this class does NOT call scene.animationController.update(dt) itself.
 * gdx-gltf's SceneManager.update(delta) already advances the AnimationController
 * for every Scene added via sceneManager.addScene() (confirmed against gdx-gltf's
 * own QuickStart example) — calling it again here would double-advance playback
 * speed. Just make sure your render loop calls sceneManager.update(delta) once
 * per frame, as shown in MarkGame.java.
 */
public class AnimationStateMachine {

    private enum ClipState { IDLE, WALK, RUN, JUMP, ATTACK, DEFEND }

    private static final String IDLE = "Idle_breathing_ready_loop";
    private static final String WALK = "Walk_in_place_loop";
    private static final String RUN = "Run_in_place_loop";
    private static final String JUMP = "Jump_arc";
    private static final String ATTACK = "Attack_forward_punch";
    private static final String DEFEND = "Defend_guard";

    private static final float BLEND_DURATION = 0.15f;

    private final Scene characterScene;
    private ClipState current = ClipState.IDLE;
    private boolean locked = false; // true during Jump/Attack/Defend — locomotion updates ignored

    public AnimationStateMachine(Scene characterScene) {
        this.characterScene = characterScene;
        play(IDLE, -1);
    }

    /** Call every frame with normalized joystick magnitude (0..1); ignored while locked. */
    public void updateLocomotion(float speedMagnitude) {
        if (locked) return;

        ClipState desired;
        if (speedMagnitude < 0.05f) {
            desired = ClipState.IDLE;
        } else if (speedMagnitude < 0.6f) {
            desired = ClipState.WALK;
        } else {
            desired = ClipState.RUN;
        }

        if (desired != current) {
            current = desired;
            play(clipNameFor(desired), -1);
        }
    }

    public void triggerJump(Runnable onLanded) {
        locked = true;
        current = ClipState.JUMP;
        playOnce(JUMP, () -> {
            locked = false;
            current = ClipState.IDLE;
            play(IDLE, -1);
            if (onLanded != null) onLanded.run();
        });
    }

    public void triggerAttack(Runnable onFinished) {
        locked = true;
        current = ClipState.ATTACK;
        playOnce(ATTACK, () -> {
            locked = false;
            current = ClipState.IDLE;
            play(IDLE, -1);
            if (onFinished != null) onFinished.run();
        });
    }

    /** Held state: call again with pressing=false to exit back to idle. */
    public void setDefending(boolean defending) {
        if (defending) {
            locked = true;
            current = ClipState.DEFEND;
            play(DEFEND, -1);
        } else if (current == ClipState.DEFEND) {
            locked = false;
            current = ClipState.IDLE;
            play(IDLE, -1);
        }
    }

    public boolean isLocked() {
        return locked;
    }

    // MARK: - Internal helpers

    private String clipNameFor(ClipState state) {
        switch (state) {
            case WALK: return WALK;
            case RUN: return RUN;
            case JUMP: return JUMP;
            case ATTACK: return ATTACK;
            case DEFEND: return DEFEND;
            default: return IDLE;
        }
    }

    private void play(String clipName, int loopCount) {
        if (characterScene.animationController == null) return;
        // Verified signature: animate(String id, int loopCount, float speed, AnimationListener listener, float transitionTime)
        characterScene.animationController.animate(clipName, loopCount, 1.0f, null, BLEND_DURATION);
    }

    private void playOnce(String clipName, Runnable onEnd) {
        if (characterScene.animationController == null) return;
        characterScene.animationController.animate(
            clipName,
            1,
            1.0f,
            new AnimationController.AnimationListener() {
                @Override public void onEnd(AnimationController.AnimationDesc animation) {
                    if (onEnd != null) onEnd.run();
                }
                @Override public void onLoop(AnimationController.AnimationDesc animation) { }
            },
            BLEND_DURATION
        );
    }
}

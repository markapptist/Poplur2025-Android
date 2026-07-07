package com.markgame.character;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * Loads ArmoredBrute_NextGen_Animated.glb and runs it through the same
 * patrol → telegraph → lunge cycle as before, now driven by the Brute's own
 * real animation clips instead of a placeholder cone that scaled up during
 * the telegraph. Clip names verified directly against the file: Idle_Combat,
 * Walk_Heavy_Loop, Attack_GroundSlam, Defend_Brace.
 *
 * Registered with the SceneManager (not the plain ModelBatch PlanetBuilder
 * primitives use) since it needs its own AnimationController to play clips —
 * same rendering path as the hero.
 */
public class EnemyController {

    private SceneAsset asset;
    public Scene scene;

    private final Vector3 planetCenter;
    private final float planetRadius;
    private float phi;
    private float theta;
    private final float patrolSpeed = 0.35f;
    private float cycleTime = 0;
    private boolean hitResolved = false;
    private boolean alive = true;
    private float deadElapsed = 0;
    private String currentClip;

    public float lungeRange = 1.6f;

    private final Vector3 position = new Vector3();
    private final Vector3 tmpUp = new Vector3();
    private final Vector3 tmpScale = new Vector3();
    private final Quaternion tmpQuat = new Quaternion();

    public interface DamageCallback {
        void onDealDamage();
    }

    /** Adds itself to sceneManager. Leaves `scene` null (check before use) if the .glb failed to load. */
    public EnemyController(SceneManager sceneManager, Vector3 planetCenter, float planetRadius, float startPhi) {
        this.planetCenter = planetCenter;
        this.planetRadius = planetRadius;
        this.phi = startPhi;
        this.theta = 0;

        try {
            asset = new GLBLoader().load(com.badlogic.gdx.Gdx.files.internal("mark_game/ArmoredBrute_NextGen_Animated.glb"));
            scene = new Scene(asset.scene);
            sceneManager.addScene(scene);
            updateTransform(1f);
            play("Idle_Combat", true);
        } catch (GdxRuntimeException e) {
            com.badlogic.gdx.Gdx.app.error("EnemyController", "Could not load ArmoredBrute_NextGen_Animated.glb — is it in assets/?", e);
        }
    }

    public void update(float dt, Vector3 characterPosition, boolean characterDefending, DamageCallback onDamage) {
        if (scene == null) return;

        if (!alive) {
            deadElapsed += dt;
            float s = Math.max(0, 1 - deadElapsed * 2);
            if (deadElapsed > 2f) {
                alive = true;
                deadElapsed = 0;
                theta = (float) (Math.random() * Math.PI * 2);
                s = 1f;
                play("Idle_Combat", true);
            }
            updateTransform(s);
            return;
        }

        cycleTime += dt;
        if (cycleTime > 5f) cycleTime = 0;

        if (cycleTime < 3.4f) {
            theta += patrolSpeed * dt;
            play("Walk_Heavy_Loop", true);
        } else if (cycleTime < 4.2f) {
            play("Attack_GroundSlam", false); // telegraph + swing is one clip
        } else {
            if (!hitResolved) {
                hitResolved = true;
                if (position.dst(characterPosition) < lungeRange && !characterDefending) {
                    onDamage.onDealDamage();
                }
            }
            if (cycleTime > 4.9f) {
                hitResolved = false;
                play("Idle_Combat", true);
            }
        }

        updateTransform(1f);
    }

    public void defeat() {
        alive = false;
        deadElapsed = 0;
        play("Defend_Brace", false); // stand-in "hit reaction" until you add a dedicated death clip
    }

    private void play(String clipName, boolean loop) {
        if (scene == null || scene.animationController == null || clipName.equals(currentClip)) return;
        scene.animationController.animate(clipName, loop ? -1 : 1, 1f, null, 0.15f);
        currentClip = clipName;
    }

    private void updateTransform(float scale) {
        GravityMath.pointOnSphere(planetCenter, planetRadius, phi, theta, position);
        GravityMath.up(position, planetCenter, tmpUp);
        GravityMath.fromToRotation(Vector3.Y, tmpUp, tmpQuat);
        tmpScale.set(scale, scale, scale);
        scene.modelInstance.transform.set(position, tmpQuat, tmpScale);
    }

    public boolean isAlive() { return alive; }
    public Vector3 getPosition() { return position; }

    public void dispose() {
        if (asset != null) asset.dispose();
    }
}

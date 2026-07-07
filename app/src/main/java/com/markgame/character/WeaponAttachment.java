package com.markgame.character;

import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.GdxRuntimeException;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * Loads the Energy Hammer + Defender Shield and "attaches" them to the hero's
 * hand bones.
 *
 * IMPORTANT ENGINE DIFFERENCE FROM THE SCENEKIT VERSION: libGDX doesn't have a
 * unified scene graph, so you can't just parent one model's node to another
 * model's bone the way SceneKit's SCNNode graph allows. The standard libGDX
 * technique (confirmed against the badlogicgames.com forum's established
 * pattern for exactly this problem) is to copy the hand bone's resolved world
 * transform onto the weapon's own Scene transform every frame, AFTER the
 * character's animation has been updated for that frame:
 *
 *   weaponScene.modelInstance.transform
 *       .set(characterScene.modelInstance.transform)
 *       .mul(handNode.globalTransform);
 *
 * Verified against the actual GLB data: both weapons' Grip_Attach_L/R marker
 * nodes sit at (0,0,0) relative to each weapon's own Root node — Root itself
 * IS the grip point, so no additional local offset is needed beyond the hand
 * transform above.
 */
public class WeaponAttachment {

    private SceneAsset hammerAsset;
    private SceneAsset shieldAsset;
    public Scene hammerScene;
    public Scene shieldScene;

    private final Matrix4 tmp = new Matrix4();

    public void attach(CharacterController character, SceneManager sceneManager) {
        try {
            hammerAsset = new GLBLoader().load(com.badlogic.gdx.Gdx.files.internal("mark_game/Weapon_Player_EnergyHammer_Animated.glb"));
            hammerScene = new Scene(hammerAsset.scene);
            sceneManager.addScene(hammerScene);
            if (hammerScene.animationController != null) {
                hammerScene.animationController.animate("Idle_Ready", -1, 1f, null, 0.15f);
            }
        } catch (GdxRuntimeException e) {
            com.badlogic.gdx.Gdx.app.error("WeaponAttachment", "Could not load Energy Hammer — is the .glb in assets/?", e);
        }

        try {
            shieldAsset = new GLBLoader().load(com.badlogic.gdx.Gdx.files.internal("mark_game/Weapon_Player_DefenderShield_Animated.glb"));
            shieldScene = new Scene(shieldAsset.scene);
            sceneManager.addScene(shieldScene);
            if (shieldScene.animationController != null) {
                shieldScene.animationController.animate("Idle_Guard", -1, 1f, null, 0.15f);
            }
        } catch (GdxRuntimeException e) {
            com.badlogic.gdx.Gdx.app.error("WeaponAttachment", "Could not load Defender Shield — is the .glb in assets/?", e);
        }
    }

    /** Call once per frame, after sceneManager.update(dt) and before sceneManager.render(). */
    public void updateTransforms(CharacterController character) {
        if (hammerScene != null) {
            Node hand = character.getRightHandNode();
            if (hand != null) {
                hammerScene.modelInstance.transform.set(character.scene.modelInstance.transform).mul(hand.globalTransform);
            }
        }
        if (shieldScene != null) {
            Node hand = character.getLeftHandNode();
            if (hand != null) {
                shieldScene.modelInstance.transform.set(character.scene.modelInstance.transform).mul(hand.globalTransform);
            }
        }
    }

    public void playHammerSwing() {
        if (hammerScene != null && hammerScene.animationController != null) {
            hammerScene.animationController.animate("Attack_HeavySwing", 1, 1f, null, 0.08f);
        }
    }

    public void playShieldGuard() {
        if (shieldScene != null && shieldScene.animationController != null) {
            shieldScene.animationController.animate("Parry_Pulse", 1, 1f, null, 0.08f);
        }
    }

    public void dispose() {
        if (hammerAsset != null) hammerAsset.dispose();
        if (shieldAsset != null) shieldAsset.dispose();
    }
}

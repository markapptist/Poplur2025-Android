package com.markgame.character;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.scene3d.scene.Scene;

/**
 * Drives Mark_NextGen_PlatformerHero_Red_Animated_Rigged.glb around the
 * surface of a spherical planet with radial gravity (Mario Galaxy style),
 * plus jump / attack / defend.
 *
 * Deliberately does not use gdx-bullet physics for movement — a constant-vector
 * gravity source doesn't represent "down is toward the planet center" as the
 * character moves around a sphere. Movement, gravity, and ground collision are
 * plain vector math here instead, mirroring the SceneKit implementation and the
 * validated in-chat preview demo.
 */
public class CharacterController {

    public final Scene scene;
    public final Vector3 planetCenter;
    public final float planetRadius;

    public float runSpeed = 3.0f;
    public float walkSpeed = 1.3f;
    public float turnSpeed = 7.0f;       // radians/sec
    public float jumpImpulse = 5.0f;
    public float launchImpulse = 7.2f;   // used by launch pads, see InteractionSystem
    public float gravity = 8.5f;
    public float attackRange = 1.3f;

    private final Vector3 position = new Vector3();
    private final Vector3 forward = new Vector3(0, 0, -1);
    private float radialVelocity = 0;
    private boolean grounded = true;
    private boolean defending = false;
    private boolean attacking = false;
    private float attackElapsed = 0;
    private boolean attackHitRegistered = false;
    private static final float ATTACK_DURATION = 0.35f;

    private final AnimationStateMachine animStateMachine;
    public EnemyController enemy;
    public Runnable onAttackLanded;
    private WeaponAttachment weapons;

    // Scratch vectors to avoid per-frame allocation churn.
    private final Vector3 tmpUp = new Vector3();
    private final Vector3 tmpRight = new Vector3();
    private final Vector3 tmpMoveDir = new Vector3();
    private final Matrix4 tmpBasis = new Matrix4();
    private final Quaternion tmpQuat = new Quaternion();

    public CharacterController(Scene scene, Vector3 planetCenter, float planetRadius) {
        this.scene = scene;
        this.planetCenter = planetCenter;
        this.planetRadius = planetRadius;
        position.set(planetCenter).add(0, planetRadius, 0);
        animStateMachine = new AnimationStateMachine(scene);
        applyTransform();
    }

    /** Bone nodes for weapon attachment — confirmed present by name in the hero's skeleton. */
    public com.badlogic.gdx.graphics.g3d.model.Node getRightHandNode() {
        return scene.modelInstance.getNode("RightHand", true);
    }

    public com.badlogic.gdx.graphics.g3d.model.Node getLeftHandNode() {
        return scene.modelInstance.getNode("LeftHand", true);
    }

    /**
     * Loads and attaches the Energy Hammer (right hand) + Defender Shield (left
     * hand). Call once after construction; pass the SceneManager so the weapons'
     * own Scenes get added for rendering + animation updates alongside the hero.
     */
    public void equipHammerAndShield(net.mgsx.gltf.scene3d.scene.SceneManager sceneManager) {
        weapons = new WeaponAttachment();
        weapons.attach(this, sceneManager);
    }

    /** Call once per frame. moveX/moveY come from your joystick, normalized -1..1. */
    public void update(float moveX, float moveY, float dt) {
        if (dt <= 0) return;

        GravityMath.up(position, planetCenter, tmpUp);
        GravityMath.reprojectTangent(forward, tmpUp);
        tmpRight.set(tmpUp).crs(forward).nor();

        if (!defending) {
            tmpMoveDir.set(tmpRight).scl(moveX).add(new Vector3(forward).scl(moveY));
            float mag = Math.min(tmpMoveDir.len(), 1f);

            if (mag > 0.02f) {
                tmpMoveDir.nor();
                float angle = GravityMath.signedAngle(forward, tmpMoveDir, tmpUp);
                float turnAmt = clamp(angle, -turnSpeed * dt, turnSpeed * dt);
                GravityMath.rotateAroundAxis(forward, tmpUp, turnAmt);
                forward.nor();

                float speed = mag > 0.6f ? runSpeed : walkSpeed;
                position.mulAdd(tmpMoveDir, speed * dt * mag);
            }
            animStateMachine.updateLocomotion(mag);
        }

        radialVelocity -= gravity * dt;
        position.mulAdd(tmpUp, radialVelocity * dt);

        float dist = new Vector3(position).sub(planetCenter).len();
        if (dist <= planetRadius) {
            Vector3 dir = new Vector3(position).sub(planetCenter).nor();
            position.set(planetCenter).mulAdd(dir, planetRadius);
            radialVelocity = 0;
            grounded = true;
        } else {
            grounded = false;
        }

        updateAttack(dt);
        applyTransform();
    }

    public void jump() {
        if (!grounded || defending || attacking) return;
        grounded = false;
        radialVelocity = jumpImpulse;
        animStateMachine.triggerJump(null);
    }

    /** Used by launch pads (see InteractionSystem) for a bigger arc than a normal jump. */
    public void launch() {
        if (!grounded) return;
        grounded = false;
        radialVelocity = launchImpulse;
        position.mulAdd(forward, 0.5f);
        animStateMachine.triggerJump(null);
    }

    public void setDefending(boolean value) {
        if (attacking) return;
        defending = value;
        animStateMachine.setDefending(value);
        if (weapons != null && value) weapons.playShieldGuard();
    }

    public void attack() {
        if (attacking || defending) return;
        attacking = true;
        attackElapsed = 0;
        attackHitRegistered = false;
        animStateMachine.triggerAttack(() -> attacking = false);
        if (weapons != null) weapons.playHammerSwing();
    }

    private void updateAttack(float dt) {
        if (!attacking) return;
        attackElapsed += dt;
        float phase = Math.min(attackElapsed / ATTACK_DURATION, 1f);

        if (!attackHitRegistered && phase >= 0.45f) {
            attackHitRegistered = true;
            if (enemy != null && enemy.isAlive() && position.dst(enemy.getPosition()) < attackRange) {
                enemy.defeat();
                if (onAttackLanded != null) onAttackLanded.run();
            }
        }
    }

    /**
     * Call AFTER sceneManager.update(dt) but BEFORE sceneManager.render() each
     * frame. gdx-gltf doesn't support parenting one Scene's nodes to another the
     * way SceneKit's unified node graph does, so weapon attachment here means:
     * read the hand bone's freshly-recalculated world transform (valid only
     * after sceneManager.update() has run this frame) and copy it onto the
     * weapon's own Scene transform. See WeaponAttachment for the verified
     * pattern this is based on.
     */
    public void syncWeaponTransforms() {
        if (weapons != null) weapons.updateTransforms(this);
    }

    public void dispose() {
        if (weapons != null) weapons.dispose();
    }

    private void applyTransform() {
        GravityMath.up(position, planetCenter, tmpUp);
        tmpRight.set(tmpUp).crs(forward).nor();
        GravityMath.basisOrientation(tmpRight, tmpUp, forward, tmpQuat);
        scene.modelInstance.transform.set(position, tmpQuat);
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public Vector3 getPosition() { return position; }
    public Vector3 getForward() { return forward; }
    public boolean isGrounded() { return grounded; }
    public boolean isDefending() { return defending; }
    public boolean isAttacking() { return attacking; }
}

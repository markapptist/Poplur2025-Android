package com.markgame.camera;

import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;
import com.markgame.character.CharacterController;
import com.markgame.character.GravityMath;

/**
 * Third-person follow camera that re-aligns to the character's LOCAL up vector
 * every frame — this is what keeps the camera stable as the character walks
 * around a sphere instead of rolling/tilting oddly near the poles.
 */
public class FollowCamera {

    public final PerspectiveCamera camera;
    public CharacterController target;

    /** Extra look-around rotation around the character's local up axis. */
    public float orbitYaw = 0f;

    public float distance = 6.2f;
    public float heightOffset = 2.1f;
    public float damping = 4.0f;

    private final Vector3 tmpUp = new Vector3();
    private final Vector3 tmpForward = new Vector3();
    private final Vector3 tmpDesired = new Vector3();
    private final Vector3 tmpLookAt = new Vector3();

    public FollowCamera(float viewportWidth, float viewportHeight) {
        camera = new PerspectiveCamera(58f, viewportWidth, viewportHeight);
        camera.near = 0.1f;
        camera.far = 200f;
    }

    public void applyOrbitDelta(float deltaYaw) {
        orbitYaw += deltaYaw;
    }

    /** Call once per frame, AFTER CharacterController.update() so it reads the latest position. */
    public void update(float dt) {
        if (target == null) return;

        Vector3 charPos = target.getPosition();
        GravityMath.up(charPos, target.planetCenter, tmpUp);

        tmpForward.set(target.getForward());
        GravityMath.reprojectTangent(tmpForward, tmpUp);
        GravityMath.rotateAroundAxis(tmpForward, tmpUp, orbitYaw);
        tmpForward.nor();

        tmpDesired.set(charPos)
            .mulAdd(tmpUp, heightOffset)
            .mulAdd(tmpForward, -distance);

        float t = Math.min(1f, dt * damping);
        camera.position.lerp(tmpDesired, t);
        camera.up.set(tmpUp);
        tmpLookAt.set(charPos).mulAdd(tmpUp, 1.0f);
        camera.lookAt(tmpLookAt);
        camera.update();
    }
}

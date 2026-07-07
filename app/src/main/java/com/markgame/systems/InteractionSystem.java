package com.markgame.systems;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;

/**
 * Proximity-based collectibles + launch pad, decoupled from how their positions
 * are chosen (see the game plan's note on later swapping this for GPS-driven
 * markers).
 */
public class InteractionSystem {

    public interface CollectListener {
        void onCollect(ModelInstance node);
    }

    public interface LaunchListener {
        void onLaunch();
    }

    private final Array<ModelInstance> collectibles = new Array<>();
    private final Array<Boolean> collectedFlags = new Array<>();
    public CollectListener onCollect;

    public ModelInstance launchPad;
    public LaunchListener onLaunch;
    private float launchCooldown = 0f;

    private final Vector3 tmpPos = new Vector3();

    public void registerCollectible(ModelInstance instance) {
        collectibles.add(instance);
        collectedFlags.add(false);
    }

    /** Call once per frame with the character's current position/grounded state. */
    public void update(float dt, Vector3 characterPosition, boolean isGrounded) {
        for (int i = 0; i < collectibles.size; i++) {
            if (collectedFlags.get(i)) continue;
            collectibles.get(i).transform.getTranslation(tmpPos);
            if (tmpPos.dst(characterPosition) < 0.8f) {
                collectedFlags.set(i, true);
                if (onCollect != null) onCollect.onCollect(collectibles.get(i));
            }
        }

        launchCooldown = Math.max(0, launchCooldown - dt);
        if (launchPad != null && isGrounded && launchCooldown <= 0f) {
            launchPad.transform.getTranslation(tmpPos);
            if (tmpPos.dst(characterPosition) < 0.9f) {
                launchCooldown = 2.2f;
                if (onLaunch != null) onLaunch.onLaunch();
            }
        }
    }

    /** Only render collectibles that haven't been picked up. */
    public boolean isCollected(int index) {
        return collectedFlags.get(index);
    }

    public Array<ModelInstance> getCollectibles() {
        return collectibles;
    }
}

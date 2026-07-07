package com.markgame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.markgame.character.GravityMath;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

/**
 * The planet + launch pad are still placeholder procedural geometry — your
 * three uploaded packs don't include environment art, so these stay as
 * simple ModelBatch-rendered primitives (swap for real art anytime; nothing
 * else depends on how they look).
 *
 * Collectibles now load the real Crystal Shard asset instead of a procedural
 * box. Unlike the planet/launch pad, it's an animated glTF asset, so it's
 * registered as a gdx-gltf Scene (via sceneManager) rather than rendered
 * through the plain ModelBatch — same rendering path as the hero/enemy.
 *
 * Keeps track of every procedurally-built Model (planet/launch pad) so they
 * can all be disposed together — call dispose() from ApplicationAdapter.dispose().
 * Collectible SceneAssets are tracked separately since they're loaded, not built.
 */
public class PlanetBuilder implements Disposable {

    private final ModelBuilder modelBuilder = new ModelBuilder();
    private final Array<Model> ownedModels = new Array<>();
    private final Array<SceneAsset> ownedSceneAssets = new Array<>();

    public ModelInstance makePlanet(float radius) {
        Material material = new Material(ColorAttribute.createDiffuse(new Color(0.29f, 0.49f, 0.35f, 1f)));
        Model model = modelBuilder.createSphere(
            radius * 2, radius * 2, radius * 2, 32, 32,
            material,
            Usage.Position | Usage.Normal
        );
        ownedModels.add(model);
        return new ModelInstance(model);
    }

    public ModelInstance makeLaunchPad(float planetRadius, float phi, float theta) {
        Material material = new Material(ColorAttribute.createDiffuse(Color.GOLD));
        Model model = modelBuilder.createCylinder(
            0.8f, 0.08f, 0.8f, 20,
            material,
            Usage.Position | Usage.Normal
        );
        ownedModels.add(model);
        ModelInstance instance = new ModelInstance(model);

        Vector3 pos = GravityMath.pointOnSphere(new Vector3(), planetRadius, phi, theta, new Vector3());
        Vector3 up = GravityMath.up(pos, new Vector3(), new Vector3());
        com.badlogic.gdx.math.Quaternion orient = new com.badlogic.gdx.math.Quaternion();
        GravityMath.fromToRotation(Vector3.Y, up, orient);
        instance.transform.set(pos, orient);
        return instance;
    }

    /**
     * Loads Projectile_CrystalShard_Animated.glb, adds it to sceneManager, and
     * plays its idle "Charge_Glitter" loop. Falls back to a small placeholder
     * gem (and returns a plain ModelInstance not registered with sceneManager)
     * if the asset isn't found, so the game still runs without it.
     */
    public ModelInstance makeCollectible(SceneManager sceneManager, float planetRadius, float phi, float theta) {
        try {
            SceneAsset asset = new GLBLoader().load(Gdx.files.internal("mark_game/Projectile_CrystalShard_Animated.glb"));
            ownedSceneAssets.add(asset);
            Scene scene = new Scene(asset.scene);
            sceneManager.addScene(scene);
            if (scene.animationController != null) {
                scene.animationController.animate("Charge_Glitter", -1, 1f, null, 0.1f);
            }
            Vector3 pos = GravityMath.pointOnSphere(new Vector3(), planetRadius, phi, theta, new Vector3());
            scene.modelInstance.transform.setToTranslation(pos);
            return scene.modelInstance;
        } catch (GdxRuntimeException e) {
            Gdx.app.error("PlanetBuilder", "Could not load Projectile_CrystalShard_Animated.glb — is it in assets/? Using placeholder gem.", e);
            Material material = new Material(
                ColorAttribute.createDiffuse(Color.YELLOW),
                ColorAttribute.createEmissive(new Color(0.35f, 0.2f, 0f, 1f))
            );
            Model model = modelBuilder.createBox(0.3f, 0.3f, 0.3f, material, Usage.Position | Usage.Normal);
            ownedModels.add(model);
            ModelInstance instance = new ModelInstance(model);
            Vector3 pos = GravityMath.pointOnSphere(new Vector3(), planetRadius, phi, theta, new Vector3());
            instance.transform.setToTranslation(pos);
            return instance;
        }
    }

    @Override
    public void dispose() {
        for (Model m : ownedModels) m.dispose();
        ownedModels.clear();
        for (SceneAsset a : ownedSceneAssets) a.dispose();
        ownedSceneAssets.clear();
    }
}

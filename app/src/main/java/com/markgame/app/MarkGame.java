package com.markgame.app;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;

import com.markgame.camera.FollowCamera;
import com.markgame.character.CharacterController;
import com.markgame.character.EnemyController;
import com.markgame.input.TouchControls;
import com.markgame.systems.InteractionSystem;
import com.markgame.world.PlanetBuilder;

/**
 * Entry point. Loads the real uploaded assets (hero, Armored Brute enemy,
 * Energy Hammer + Defender Shield, Crystal Shard collectibles) instead of
 * placeholders, on the same single-planet spherical-gravity design as before.
 *
 * Rendering is split in two:
 *   sceneManager  -> hero, enemy, hammer, shield, collectibles (all animated
 *                    glTF assets — sceneManager.update(dt) advances every one
 *                    of their AnimationControllers, sceneManager.render()
 *                    draws them all with PBR)
 *   modelBatch    -> planet + launch pad (plain procedural primitives, no
 *                    animation, not covered by your uploaded packs)
 */
public class MarkGame extends ApplicationAdapter {

    private static final float PLANET_RADIUS = 3.0f;

    private SceneManager sceneManager;
    private SceneAsset characterAsset;
    private Scene characterScene;
    private CharacterController character;

    private PlanetBuilder planetBuilder;
    private ModelInstance planetInstance;
    private ModelInstance launchPadInstance;
    private EnemyController enemy;

    private FollowCamera followCamera;
    private InteractionSystem interactionSystem;
    private TouchControls touchControls;

    private ModelBatch modelBatch;
    private Environment worldEnvironment;

    private Stage hudStage;
    private Label heartsLabel;
    private Label collectedLabel;

    private int hearts = 3;
    private int collected = 0;
    private float elapsedTime = 0f;
    private float lastDamageTime = -999f;

    private float moveX = 0f;
    private float moveY = 0f;

    @Override
    public void create() {
        // --- Character (gdx-gltf) ---
        // Requires Mark_NextGen_PlatformerHero_Red_Animated_Rigged.glb in assets/mark_game/.
        characterAsset = new GLBLoader().load(Gdx.files.internal("mark_game/Mark_NextGen_PlatformerHero_Red_Animated_Rigged.glb"));
        characterScene = new Scene(characterAsset.scene);

        sceneManager = new SceneManager();
        sceneManager.addScene(characterScene);
        sceneManager.setAmbientLight(0.5f);
        DirectionalLightEx sun = new DirectionalLightEx();
        sun.direction.set(-1f, -2f, -1f).nor();
        sun.color.set(Color.WHITE);
        sceneManager.environment.add(sun);

        followCamera = new FollowCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        sceneManager.setCamera(followCamera.camera);

        character = new CharacterController(characterScene, new Vector3(0, 0, 0), PLANET_RADIUS);
        character.equipHammerAndShield(sceneManager); // loads + registers hammer/shield Scenes
        followCamera.target = character;

        // --- Enemy (real Armored Brute asset, also gdx-gltf) ---
        enemy = new EnemyController(sceneManager, new Vector3(0, 0, 0), PLANET_RADIUS, 1.15f);
        character.enemy = enemy;
        character.onAttackLanded = this::pulseCollectedLabel;

        // --- World (planet/launch pad still placeholder — see PlanetBuilder) ---
        planetBuilder = new PlanetBuilder();
        planetInstance = planetBuilder.makePlanet(PLANET_RADIUS);
        launchPadInstance = planetBuilder.makeLaunchPad(PLANET_RADIUS, 2.0f, 3.6f);

        interactionSystem = new InteractionSystem();
        interactionSystem.launchPad = launchPadInstance;
        interactionSystem.onLaunch = () -> character.launch();
        interactionSystem.onCollect = node -> {
            collected++;
            collectedLabel.setText(collected + "/3");
            // Scenes registered with sceneManager render unconditionally each
            // frame — "hiding" a collected item means shrinking it to zero
            // rather than skipping a draw call (see EnemyController.defeat()
            // for the same technique used on enemy death).
            node.transform.scale(0f, 0f, 0f);
        };

        float[][] collectSpots = {{0.9f, 2.4f}, {1.6f, 4.8f}, {2.3f, 1.0f}};
        for (float[] spot : collectSpots) {
            ModelInstance c = planetBuilder.makeCollectible(sceneManager, PLANET_RADIUS, spot[0], spot[1]);
            interactionSystem.registerCollectible(c);
        }

        modelBatch = new ModelBatch();
        worldEnvironment = new Environment();
        worldEnvironment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.5f, 1f));
        worldEnvironment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -2f, -1f));

        // --- Input (joystick + jump/attack/defend) ---
        touchControls = new TouchControls(new TouchControls.Listener() {
            @Override public void onMove(float x, float y) { moveX = x; moveY = y; }
            @Override public void onJump() { character.jump(); }
            @Override public void onAttack() { character.attack(); }
            @Override public void onDefendChanged(boolean pressing) { character.setDefending(pressing); }
        });

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(touchControls.stage);
        Gdx.input.setInputProcessor(multiplexer);

        // --- HUD ---
        hudStage = new Stage(new ScreenViewport());
        Label.LabelStyle style = new Label.LabelStyle(new BitmapFont(), Color.WHITE);
        heartsLabel = new Label("Hearts: 3", style);
        heartsLabel.setPosition(16, Gdx.graphics.getHeight() - 32);
        collectedLabel = new Label("0/3", style);
        collectedLabel.setPosition(140, Gdx.graphics.getHeight() - 32);
        hudStage.addActor(heartsLabel);
        hudStage.addActor(collectedLabel);
    }

    @Override
    public void render() {
        float dt = Gdx.graphics.getDeltaTime();
        elapsedTime += dt;

        Gdx.gl.glClearColor(0.05f, 0.06f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        character.update(moveX, moveY, dt);
        followCamera.update(dt);

        enemy.update(dt, character.getPosition(), character.isDefending(), () -> {
            if (elapsedTime - lastDamageTime > 1.5f) {
                lastDamageTime = elapsedTime;
                hearts = Math.max(0, hearts - 1);
                heartsLabel.setText("Hearts: " + hearts);
            }
        });

        interactionSystem.update(dt, character.getPosition(), character.isGrounded());

        // Advances animation for every registered Scene (hero, enemy, hammer,
        // shield, collectibles) and recalculates their node global transforms.
        sceneManager.update(dt);

        // MUST run after sceneManager.update() so hand-bone globalTransforms
        // are current for this frame — see CharacterController.syncWeaponTransforms().
        character.syncWeaponTransforms();

        sceneManager.render();

        // Placeholder world geometry (plain libGDX primitives, not animated).
        modelBatch.begin(followCamera.camera);
        modelBatch.render(planetInstance, worldEnvironment);
        modelBatch.render(launchPadInstance, worldEnvironment);
        modelBatch.end();

        // UI
        touchControls.update();
        touchControls.stage.draw();
        hudStage.act(dt);
        hudStage.draw();
    }

    private void pulseCollectedLabel() {
        if (collectedLabel == null) return;
        collectedLabel.clearActions();
        collectedLabel.setColor(Color.YELLOW);
        collectedLabel.setFontScale(1.25f);
        collectedLabel.addAction(Actions.parallel(
                Actions.color(Color.WHITE, 0.25f),
                Actions.scaleTo(1f, 1f, 0.25f)
        ));
    }

    @Override
    public void resize(int width, int height) {
        sceneManager.updateViewport(width, height);
        followCamera.camera.viewportWidth = width;
        followCamera.camera.viewportHeight = height;
        followCamera.camera.update();
        touchControls.resize(width, height);
        hudStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        sceneManager.dispose();
        characterAsset.dispose();
        character.dispose();
        planetBuilder.dispose();
        enemy.dispose();
        modelBatch.dispose();
        touchControls.dispose();
        hudStage.dispose();
    }
}

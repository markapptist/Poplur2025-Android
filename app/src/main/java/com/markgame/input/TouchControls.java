package com.markgame.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Movement joystick (bottom-left) + Jump/Attack/Defend buttons (bottom-right)
 * built from Scene2D UI. Uses a minimal generated skin so this drops into any
 * project without needing an external skin/atlas file — swap `buildSkin()` for
 * your own UI skin whenever you're ready to reskin it.
 */
public class TouchControls {

    public final Stage stage;
    private final Touchpad touchpad;

    public interface Listener {
        void onMove(float x, float y);
        void onJump();
        void onAttack();
        void onDefendChanged(boolean pressing);
    }

    public TouchControls(Listener listener) {
        stage = new Stage(new ScreenViewport());
        Skin skin = buildSkin();

        Touchpad.TouchpadStyle padStyle = new Touchpad.TouchpadStyle();
        padStyle.background = skin.getDrawable("touchpad-bg");
        padStyle.knob = skin.getDrawable("touchpad-knob");
        touchpad = new Touchpad(10f, padStyle);
        touchpad.setBounds(24, 24, 120, 120);
        stage.addActor(touchpad); // Touchpad handles its own input; we read knob percent each frame in update()

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.up = skin.getDrawable("button-bg");
        buttonStyle.down = skin.getDrawable("button-bg-pressed");
        buttonStyle.font = skin.getFont("default-font");

        TextButton jumpButton = new TextButton("JUMP", buttonStyle);
        jumpButton.setSize(64, 54);
        jumpButton.setPosition(Gdx.graphics.getWidth() - 76, 24);
        jumpButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { listener.onJump(); }
        });
        stage.addActor(jumpButton);

        TextButton attackButton = new TextButton("ATK", buttonStyle);
        attackButton.setSize(64, 54);
        attackButton.setPosition(Gdx.graphics.getWidth() - 148, 24);
        attackButton.addListener(new ClickListener() {
            @Override public void clicked(InputEvent event, float x, float y) { listener.onAttack(); }
        });
        stage.addActor(attackButton);

        TextButton defendButton = new TextButton("DEF", buttonStyle);
        defendButton.setSize(64, 54);
        defendButton.setPosition(Gdx.graphics.getWidth() - 220, 24);
        defendButton.addListener(new ClickListener() {
            @Override public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                listener.onDefendChanged(true);
                return super.touchDown(event, x, y, pointer, button);
            }
            @Override public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                listener.onDefendChanged(false);
                super.touchUp(event, x, y, pointer, button);
            }
        });
        stage.addActor(defendButton);

        this.moveListener = listener;
    }

    private final Listener moveListener;

    /** Call once per frame to push the joystick's current position to the listener. */
    public void update() {
        moveListener.onMove(touchpad.getKnobPercentX(), touchpad.getKnobPercentY());
        stage.act();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private Skin buildSkin() {
        Skin skin = new Skin();

        Pixmap circlePixmap = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        circlePixmap.setColor(1, 1, 1, 0.15f);
        circlePixmap.fillCircle(64, 64, 64);
        skin.add("touchpad-bg", new Texture(circlePixmap));
        circlePixmap.dispose();

        Pixmap knobPixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        knobPixmap.setColor(1, 1, 1, 0.4f);
        knobPixmap.fillCircle(32, 32, 32);
        skin.add("touchpad-knob", new Texture(knobPixmap));
        knobPixmap.dispose();

        Pixmap buttonPixmap = new Pixmap(64, 54, Pixmap.Format.RGBA8888);
        buttonPixmap.setColor(1, 1, 1, 0.15f);
        buttonPixmap.fillRectangle(0, 0, 64, 54);
        skin.add("button-bg", new Texture(buttonPixmap));
        buttonPixmap.dispose();

        Pixmap buttonPressedPixmap = new Pixmap(64, 54, Pixmap.Format.RGBA8888);
        buttonPressedPixmap.setColor(1, 1, 1, 0.35f);
        buttonPressedPixmap.fillRectangle(0, 0, 64, 54);
        skin.add("button-bg-pressed", new Texture(buttonPressedPixmap));
        buttonPressedPixmap.dispose();

        skin.add("default-font", new BitmapFont());

        // Skin.getDrawable(name) auto-converts a registered Texture/TextureRegion into a
        // Drawable if no Drawable is explicitly registered under that name — no extra step needed.
        return skin;
    }

    public void dispose() {
        stage.dispose();
    }
}

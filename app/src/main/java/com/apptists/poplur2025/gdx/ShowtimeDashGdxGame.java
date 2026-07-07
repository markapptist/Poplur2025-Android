package com.apptists.poplur2025.gdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;

public class ShowtimeDashGdxGame extends ApplicationAdapter {
    private static final float PLAYER_X = 0.22f;
    private static final float GROUND_Y = 0.22f;

    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private BitmapFont font;
    private final Array<Obstacle> obstacles = new Array<>();
    private final Array<Pickup> pickups = new Array<>();

    private float playerY;
    private float playerVy;
    private float speed;
    private float spawnTimer;
    private float pickupTimer;
    private float countdown;
    private int score;
    private int lives;
    private int jumpsRemaining;
    private boolean running;
    private boolean gameOver;

    @Override
    public void create() {
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.45f);
        reset();
    }

    @Override
    public void render() {
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        handleInput();
        update(dt);
        draw();
    }

    private void handleInput() {
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (gameOver) {
                reset();
            } else if (running && jumpsRemaining > 0) {
                playerVy = jumpsRemaining == 2 ? 860f : 700f;
                jumpsRemaining--;
            }
        }
    }

    private void update(float dt) {
        if (!running) {
            countdown -= dt;
            if (countdown <= 0f) running = true;
            return;
        }
        if (gameOver) return;

        float h = Gdx.graphics.getHeight();
        float ground = h * GROUND_Y;
        playerVy -= 1550f * dt;
        playerY += playerVy * dt;
        if (playerY <= ground + 52f) {
            playerY = ground + 52f;
            playerVy = 0f;
            jumpsRemaining = 2;
        }

        speed = Math.min(780f, speed + 9f * dt);
        spawnTimer += dt;
        pickupTimer += dt;
        score += (int)(dt * 56f);

        if (spawnTimer > Math.max(0.62f, 1.18f - speed / 860f)) {
            spawnTimer = 0f;
            obstacles.add(new Obstacle(Gdx.graphics.getWidth() + 80f, MathUtils.randomBoolean()));
        }
        if (pickupTimer > 0.82f) {
            pickupTimer = 0f;
            pickups.add(new Pickup(
                Gdx.graphics.getWidth() + 70f,
                ground + MathUtils.random(92f, 178f)
            ));
        }

        Rectangle player = playerRect();
        for (int i = obstacles.size - 1; i >= 0; i--) {
            Obstacle o = obstacles.get(i);
            o.x -= speed * dt;
            if (o.x < -100f) {
                obstacles.removeIndex(i);
            } else if (player.overlaps(o.rect(ground))) {
                obstacles.removeIndex(i);
                lives--;
                if (lives <= 0) gameOver = true;
            }
        }

        for (int i = pickups.size - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.x -= speed * dt;
            if (p.x < -80f) {
                pickups.removeIndex(i);
            } else if (player.overlaps(p.rect())) {
                pickups.removeIndex(i);
                score += 250;
            }
        }
    }

    private Rectangle playerRect() {
        float w = Gdx.graphics.getWidth();
        return new Rectangle(w * PLAYER_X - 24f, playerY - 50f, 48f, 92f);
    }

    private void draw() {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        float ground = h * GROUND_Y;

        Gdx.gl.glClearColor(0.03f, 0.08f, 0.16f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.rect(0, 0, w, h, new Color(0.03f, 0.08f, 0.16f, 1f), new Color(0.07f, 0.14f, 0.28f, 1f), new Color(0.04f, 0.11f, 0.23f, 1f), new Color(0.02f, 0.06f, 0.12f, 1f));
        drawHills(w, h, 0.36f, new Color(0.09f, 0.18f, 0.34f, 1f));
        drawHills(w, h, 0.29f, new Color(0.06f, 0.12f, 0.25f, 1f));
        shapes.setColor(0.70f, 0.12f, 0.16f, 1f);
        shapes.rect(0, 0, w, ground);
        shapes.setColor(1f, 0.82f, 0.28f, 1f);
        shapes.rect(0, ground + 3f, w, 8f);

        drawPlayer(w * PLAYER_X, playerY);
        for (Obstacle o : obstacles) drawObstacle(o, ground);
        for (Pickup p : pickups) drawPickup(p);
        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Score " + score + "   Lives " + lives, 24f, h - 72f);
        font.getData().setScale(1.0f);
        font.setColor(1f, 1f, 1f, 0.55f);
        font.draw(batch, "Tap to jump. Double tap for air control.", 24f, h - 104f);
        font.getData().setScale(1.45f);
        if (!running) {
            font.getData().setScale(3.8f);
            font.setColor(1f, 0.82f, 0.28f, 1f);
            String mark = countdown > 2.4f ? "3" : countdown > 1.5f ? "2" : countdown > 0.65f ? "1" : "GO";
            font.draw(batch, mark, w / 2f - 42f, h * 0.58f);
            font.getData().setScale(1.45f);
        } else if (gameOver) {
            font.getData().setScale(2.35f);
            font.setColor(Color.WHITE);
            font.draw(batch, "SHOW OVER", w / 2f - 150f, h * 0.58f);
            font.getData().setScale(1.3f);
            font.setColor(1f, 1f, 1f, 0.72f);
            font.draw(batch, "Tap to retry", w / 2f - 76f, h * 0.52f);
            font.getData().setScale(1.45f);
        }
        batch.end();
    }

    private void drawHills(float w, float h, float base, Color color) {
        shapes.setColor(color);
        float y = h * base;
        for (int i = -1; i < 7; i++) {
            shapes.ellipse(i * w / 5f, y - 74f, w / 2.3f, 170f);
        }
    }

    private void drawPlayer(float x, float y) {
        shapes.setColor(0.13f, 0.42f, 0.95f, 1f);
        shapes.rect(x - 24f, y - 44f, 48f, 58f);
        shapes.setColor(0.98f, 0.86f, 0.64f, 1f);
        shapes.circle(x, y + 34f, 25f);
        shapes.setColor(0.79f, 0.07f, 0.13f, 1f);
        shapes.rect(x - 30f, y + 55f, 60f, 16f);
        shapes.setColor(Color.WHITE);
        shapes.circle(x - 8f, y + 38f, 4f);
        shapes.circle(x + 8f, y + 38f, 4f);
    }

    private void drawObstacle(Obstacle o, float ground) {
        if (o.enemy) {
            shapes.setColor(0.94f, 0.12f, 0.23f, 1f);
            shapes.circle(o.x, ground + 32f, 28f);
            shapes.setColor(Color.WHITE);
            shapes.circle(o.x - 9f, ground + 38f, 5f);
            shapes.circle(o.x + 9f, ground + 38f, 5f);
        } else {
            shapes.setColor(0.45f, 0.20f, 0.09f, 1f);
            shapes.rect(o.x - 28f, ground + 6f, 56f, 56f);
        }
    }

    private void drawPickup(Pickup p) {
        shapes.setColor(1f, 0.84f, 0.16f, 1f);
        shapes.circle(p.x, p.y, 19f);
        shapes.setColor(1f, 1f, 1f, 0.82f);
        shapes.circle(p.x, p.y, 8f);
    }

    private void reset() {
        float ground = Math.max(1, Gdx.graphics.getHeight()) * GROUND_Y;
        playerY = ground + 52f;
        playerVy = 0f;
        speed = 430f;
        spawnTimer = 0f;
        pickupTimer = 0f;
        countdown = 3.25f;
        score = 0;
        lives = 3;
        jumpsRemaining = 2;
        running = false;
        gameOver = false;
        obstacles.clear();
        pickups.clear();
    }

    @Override
    public void dispose() {
        shapes.dispose();
        batch.dispose();
        font.dispose();
    }

    private static final class Obstacle {
        float x;
        final boolean enemy;
        Obstacle(float x, boolean enemy) {
            this.x = x;
            this.enemy = enemy;
        }
        Rectangle rect(float ground) {
            return new Rectangle(x - 28f, ground + 6f, 56f, 56f);
        }
    }

    private static final class Pickup {
        float x;
        final float y;
        Pickup(float x, float y) {
            this.x = x;
            this.y = y;
        }
        Rectangle rect() {
            return new Rectangle(x - 20f, y - 20f, 40f, 40f);
        }
    }
}

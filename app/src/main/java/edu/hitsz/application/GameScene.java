package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.basic.AbstractFlyingObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Shared renderer and round controller. The worker alone mutates GameWorld. */
public abstract class GameScene extends BaseGame {
    public static final int MSG_ROUND_UPDATE = 1;
    private final Difficulty difficulty;
    private final Handler resultHandler;
    private final AtomicBoolean restartRequested = new AtomicBoolean();
    private GameWorld world;
    private int round;
    private volatile RoundUpdate roundState = new RoundUpdate(0, 0, false, false, List.of());
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint effect = new Paint(Paint.ANTI_ALIAS_FLAG);
    private volatile Snapshot snapshot = new Snapshot(300, 817, 300, 0, 0, 0, 0, false);
    protected GameScene(Context context, Difficulty difficulty, int backgroundId, Handler resultHandler) {
        super(context, backgroundId);
        this.difficulty = difficulty;
        this.resultHandler = resultHandler;
        world = new GameWorld(difficulty);
    }
    public final Difficulty difficulty() { return difficulty; }
    public final void requestRestart() { restartRequested.set(true); }

    @Override protected void onTick(TouchState input, Viewport viewport) {
        if (restartRequested.getAndSet(false) && world.isGameOver()) {
            world = new GameWorld(difficulty);
            round++;
            input = TouchState.NONE;
        }
        world.tick(input, viewport);
        snapshot = new Snapshot(world.hero.getLocationX(), world.hero.getLocationY(), world.hero.getHp(),
                world.score(), world.ticks(), world.enemies().size(), world.heroBullets().size(), world.isGameOver());
        List<GameEvent> events = world.drainEvents();
        RoundUpdate previous = roundState;
        boolean boss = world.isBossActive(), over = world.isGameOver();
        if (round != previous.round || boss != previous.boss || over != previous.gameOver || !events.isEmpty()) {
            RoundUpdate update = new RoundUpdate(round, world.score(), boss, over, events);
            roundState = update;
            resultHandler.obtainMessage(MSG_ROUND_UPDATE, update).sendToTarget();
        }
    }

    /** Immutable worker-to-main batch, also usable after the worker has been joined. */
    public final RoundUpdate roundState() { return roundState; }
    public static final class RoundUpdate {
        public final int round, score;
        public final boolean boss, gameOver;
        public final List<GameEvent> events;
        private RoundUpdate(int round, int score, boolean boss, boolean gameOver, List<GameEvent> events) {
            this.round = round; this.score = score; this.boss = boss; this.gameOver = gameOver;
            this.events = Collections.unmodifiableList(new ArrayList<>(events));
        }
    }
    @Override protected void onDrawWorld(Canvas canvas) {
        drawBackground(canvas, world.backgroundOffset());
        drawObjects(canvas, world.enemyBullets());
        drawObjects(canvas, world.heroBullets());
        drawObjects(canvas, world.enemies());
        drawObjects(canvas, world.props());
        drawSprite(canvas, Sprite.HERO, world.hero.getLocationX(), world.hero.getLocationY());
        // Match the original AbstractGame.paintScoreAndLife(): red text, same anchors and size.
        text.setTextAlign(Paint.Align.LEFT);
        text.setColor(Color.RED); text.setTextSize(22); text.setFakeBoldText(true);
        canvas.drawText("SCORE:" + world.score(), 10, 25, text);
        canvas.drawText("LIFE:" + world.hero.getHp(), 10, 50, text);
        canvas.drawText("DIFFICULTY:" + difficulty.hudName, 10, 75, text);
        if (world.difficultyTier() > 0) canvas.drawText("TIER:" + world.difficultyTier(), 10, 100, text);
        drawBossHpBar(canvas);
        if (world.isGameOver()) drawGameOver(canvas);
    }

    /** Original JavaFX Boss HUD translated to Canvas. */
    private void drawBossHpBar(Canvas canvas) {
        BossEnemy boss = world.activeBoss();
        if (boss == null) return;
        effect.setStyle(Paint.Style.FILL);
        effect.setColor(Color.rgb(50, 50, 50));
        canvas.drawRect(118, 28, 482, 52, effect);
        effect.setColor(Color.BLACK);
        canvas.drawRect(120, 30, 480, 50, effect);
        double ratio = boss.getHpRatio();
        int red = ratio > .5 ? (int)((ratio - .5) * 510) : 255;
        int green = ratio > .5 ? 255 : (int)(ratio * 510);
        effect.setColor(Color.rgb(red, green, 0));
        canvas.drawRect(120, 30, 120 + 360 * (float) ratio, 50, effect);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTextSize(14); text.setFakeBoldText(true); text.setColor(Color.WHITE);
        canvas.drawText("BOSS  HP: " + boss.getHp() + " / " + boss.maxHp, 300, 45, text);
    }

    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            drawSprite(canvas, object.getSprite(), object.getLocationX(), object.getLocationY());
            if (object.isFrozen()) {
                drawFreezeOverlay(canvas, object.getLocationX() - object.getWidth() / 2f,
                        object.getLocationY() - object.getHeight() / 2f, object.getWidth(), object.getHeight());
            }
        }
    }

    /** Colors, line widths and geometry translated from the original JavaFX renderer. */
    private void drawFreezeOverlay(Canvas canvas, float x, float y, float w, float h) {
        effect.setStyle(Paint.Style.FILL);
        effect.setColor(Color.argb(89, 80, 180, 255));
        canvas.drawRect(x, y, x + w, y + h, effect);
        effect.setStyle(Paint.Style.STROKE);
        effect.setColor(Color.argb(230, 140, 210, 255)); effect.setStrokeWidth(2.5f);
        canvas.drawRoundRect(new RectF(x + 1, y + 1, x + w - 1, y + h - 1), 3, 3, effect);
        effect.setColor(Color.argb(128, 200, 235, 255)); effect.setStrokeWidth(1);
        canvas.drawRoundRect(new RectF(x + 3, y + 3, x + w - 3, y + h - 3), 2, 2, effect);
        float cx = x + w / 2, cy = y + h / 2;
        effect.setColor(Color.argb(115, 200, 230, 255)); effect.setStrokeWidth(1.2f);
        canvas.drawLine(cx, y + 4, cx, y + h - 4, effect);
        canvas.drawLine(x + 4, cy, x + w - 4, cy, effect);
        canvas.drawLine(x + 4, y + 4, x + w - 4, y + h - 4, effect);
        canvas.drawLine(x + w - 4, y + 4, x + 4, y + h - 4, effect);
        effect.setStyle(Paint.Style.FILL); effect.setColor(Color.argb(77, 255, 255, 255));
        canvas.drawOval(new RectF(cx - 3, cy - 3, cx + 3, cy + 3), effect);
        effect.setColor(Color.argb(51, 255, 255, 255));
        canvas.drawOval(new RectF(x + w * .25f, y + h * .3f, x + w * .25f + 3, y + h * .3f + 3), effect);
        canvas.drawOval(new RectF(x + w * .75f, y + h * .6f, x + w * .75f + 3, y + h * .6f + 3), effect);
    }

    private void drawGameOver(Canvas canvas) {
        text.setColor(Color.argb(153, 0, 0, 0));
        canvas.drawRect(0, 0, 600, 900, text);
        text.setTextAlign(Paint.Align.CENTER); text.setTextSize(60); text.setFakeBoldText(true);
        text.setColor(Color.argb(128, 150, 0, 0));
        canvas.drawText("GAME OVER", 303, 433, text);
        text.setColor(Color.rgb(255, 30, 30));
        canvas.drawText("GAME OVER", 300, 430, text);
        text.setTextSize(20); text.setFakeBoldText(false); text.setColor(Color.rgb(220, 220, 220));
        canvas.drawText("请选择重新开始或返回菜单", 300, 490, text);
        text.setTextSize(24); text.setFakeBoldText(true); text.setColor(Color.rgb(255, 215, 0));
        canvas.drawText("最终得分: " + world.score(), 300, 380, text);
    }

    /** Immutable observation; device tests cannot modify the live world. */
    public Snapshot snapshot() { return snapshot; }
    public static final class Snapshot {
        public final int heroX, heroY, hp, score, ticks, enemies, bullets;
        public final boolean gameOver;
        Snapshot(int x, int y, int hp, int score, int ticks, int enemies, int bullets, boolean over) {
            heroX = x; heroY = y; this.hp = hp; this.score = score; this.ticks = ticks;
            this.enemies = enemies; this.bullets = bullets; gameOver = over;
        }
    }
}

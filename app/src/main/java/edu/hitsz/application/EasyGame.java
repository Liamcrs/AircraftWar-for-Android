package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import edu.hitsz.basic.AbstractFlyingObject;
import java.util.List;

/** Default Easy mode; game logic lives in GameWorld, rendering stays on the worker. */
public final class EasyGame extends BaseGame {
    private GameWorld world = new GameWorld();
    private long lastGesture = -1;
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private volatile Snapshot snapshot = new Snapshot(300, 817, 300, 0, 0, 0, 0, false);
    public EasyGame(Context context) { super(context); }
    @Override protected void onTick(TouchState input, Viewport viewport) {
        boolean newGesture = input.active && input.gesture != lastGesture;
        if (input.active) lastGesture = input.gesture;
        if (world.isGameOver() && newGesture && input.viewportVersion == viewport.version) {
            world = new GameWorld();
            input = TouchState.NONE;
        }
        world.tick(input, viewport);
        snapshot = new Snapshot(world.hero.getLocationX(), world.hero.getLocationY(), world.hero.getHp(),
                world.score(), world.ticks(), world.enemies().size(), world.heroBullets().size(), world.isGameOver());
    }
    @Override protected void onDrawWorld(Canvas canvas) {
        drawBackground(canvas, world.backgroundOffset());
        drawObjects(canvas, world.enemyBullets());
        drawObjects(canvas, world.heroBullets());
        drawObjects(canvas, world.enemies());
        drawObjects(canvas, world.props());
        drawSprite(canvas, Sprite.HERO, world.hero.getLocationX(), world.hero.getLocationY());
        text.setTextAlign(Paint.Align.LEFT);
        text.setColor(0xc0101b27);
        canvas.drawRoundRect(new RectF(12, 12, 245, 118), 12, 12, text);
        text.setColor(Color.WHITE); text.setTextSize(22); text.setFakeBoldText(true);
        canvas.drawText("SCORE  " + world.score(), 28, 43, text);
        canvas.drawText("LIFE     " + world.hero.getHp(), 28, 74, text);
        text.setColor(0xff7fd6fa); text.setTextSize(16);
        canvas.drawText("EASY  ·  按住飞机拖动", 28, 103, text);
        if (world.isGameOver()) {
            text.setColor(0xbb101b27); canvas.drawRect(0, 0, 600, 900, text);
            text.setTextAlign(Paint.Align.CENTER); text.setColor(Color.WHITE); text.setTextSize(54);
            canvas.drawText("GAME OVER", 300, 410, text);
            text.setColor(0xff7fd6fa); text.setTextSize(28);
            canvas.drawText("最终得分  " + world.score(), 300, 465, text);
            text.setColor(0xffeeeeee); text.setTextSize(20);
            canvas.drawText("点击画面，开始新一局", 300, 515, text);
        }
    }

    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            drawSprite(canvas, object.getSprite(), object.getLocationX(), object.getLocationY());
            if (object.isFrozen()) {
                paint.setColor(0x6680ddff);
                canvas.drawRect(object.getLocationX() - object.getWidth() / 2f,
                        object.getLocationY() - object.getHeight() / 2f,
                        object.getLocationX() + object.getWidth() / 2f,
                        object.getLocationY() + object.getHeight() / 2f, paint);
                paint.setColor(Color.WHITE);
            }
        }
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

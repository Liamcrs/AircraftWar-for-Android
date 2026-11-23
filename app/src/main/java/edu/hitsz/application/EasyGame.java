package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;

/** Stage 3 checkpoint: actual SurfaceView loop, original 600x900 coordinates. */
public final class EasyGame extends BaseGame {
    private int backgroundOffset;
    public EasyGame(Context context) { super(context); }
    @Override protected void onTick(TouchState input, Viewport viewport) {
        backgroundOffset = (backgroundOffset + 2) % GameConfig.WINDOW_HEIGHT;
    }
    @Override protected void onDrawWorld(Canvas canvas) {
        drawBackground(canvas, backgroundOffset);
        drawSprite(canvas, Sprite.HERO, 300, 817);
    }
}

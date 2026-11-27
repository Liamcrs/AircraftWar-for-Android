package edu.hitsz.application;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.util.concurrent.atomic.AtomicInteger;

/** All callbacks run on the UI thread; only run() owns and updates the game world. */
public abstract class BaseGame extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private static final String TAG = "AircraftGame";
    private static final AtomicInteger LIVE_LOOPS = new AtomicInteger();
    protected final ImageManager images;
    protected final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final SurfaceHolder surfaceHolder;
    private volatile boolean running;
    private boolean resumed, surfaceReady;
    private Thread gameThread;
    private volatile Viewport viewport;
    private volatile long renderedFrames;
    private volatile TouchState touch = TouchState.NONE;
    private int viewportVersion, pointerId = -1;
    private long gestureId;
    private float downX, downY;

    protected BaseGame(Context context, int backgroundId) {
        super(context);
        images = new ImageManager(backgroundId);
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);
        setContentDescription("飞机大战，按住英雄机拖动，自动发射子弹");
    }

    public final void resumeGame() { resumed = true; Log.i(TAG, "Activity resumed"); startLoopIfReady(); }
    public final void pauseGame() { resumed = false; clearTouch(); stopLoopAndWait(); Log.i(TAG, "Activity paused; worker joined"); }

    @Override public final void surfaceCreated(SurfaceHolder holder) {
        surfaceReady = true;
        Log.i(TAG, "Surface created");
        images.load(getResources());
        startLoopIfReady();
    }

    @Override public final void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        clearTouch();
        viewport = width > 0 && height > 0 ? new Viewport(width, height, ++viewportVersion) : null;
        Log.i(TAG, "Surface changed: " + width + "x" + height);
        startLoopIfReady();
    }

    @Override public final void surfaceDestroyed(SurfaceHolder holder) {
        surfaceReady = false;
        Log.i(TAG, "Surface destroyed");
        clearTouch();
        stopLoopAndWait();
        viewport = null;
    }

    private void startLoopIfReady() {
        if (!resumed || !surfaceReady || viewport == null || images.background() == null) return;
        if (gameThread != null && gameThread.isAlive()) return;
        running = true;
        gameThread = new Thread(this, "AircraftGame-" + Integer.toHexString(System.identityHashCode(this)));
        gameThread.start();
    }

    private void stopLoopAndWait() {
        running = false;
        Thread previous = gameThread;
        if (previous == null) return;
        if (previous == Thread.currentThread()) throw new IllegalStateException("Cannot join game thread itself");
        previous.interrupt();
        boolean interrupted = false;
        // Worker never waits for UI or does blocking I/O. No locks are held while joining.
        while (previous.isAlive()) {
            try { previous.join(); }
            catch (InterruptedException exception) { interrupted = true; }
        }
        gameThread = null;
        if (interrupted) Thread.currentThread().interrupt();
    }

    @Override public final void run() {
        int count = LIVE_LOOPS.incrementAndGet();
        Log.i(TAG, "START " + Thread.currentThread().getName() + " live=" + count);
        long previous = System.nanoTime();
        long accumulator = 0;
        final long tick = GameConfig.TICK_MS * 1_000_000L;
        try {
            while (running && !Thread.currentThread().isInterrupted()) {
                long frameStart = System.nanoTime();
                accumulator += Math.min(frameStart - previous, 250_000_000L);
                previous = frameStart;
                Viewport frameViewport = viewport;
                if (frameViewport != null) {
                    int steps = 0;
                    while (accumulator >= tick && steps < 5 && running) {
                        onTick(touch, frameViewport);
                        accumulator -= tick;
                        steps++;
                    }
                    if (steps == 5) accumulator %= tick;
                    if (running) drawFrame(frameViewport);
                }
                long remaining = 16_666_667L - (System.nanoTime() - frameStart);
                if (remaining > 0) Thread.sleep(remaining / 1_000_000L, (int) (remaining % 1_000_000L));
            }
        } catch (InterruptedException expectedOnStop) {
            Thread.currentThread().interrupt();
        } catch (RuntimeException failure) {
            Log.e(TAG, "Game loop failed", failure);
        } finally {
            running = false;
            Log.i(TAG, "STOP " + Thread.currentThread().getName() + " live=" + LIVE_LOOPS.decrementAndGet());
        }
    }

    private void drawFrame(Viewport view) {
        Canvas canvas = null;
        try {
            canvas = surfaceHolder.lockCanvas();
            if (canvas == null) return;
            canvas.drawColor(0xff101b27);
            int saved = canvas.save();
            canvas.translate(view.offsetX, view.offsetY);
            canvas.scale(view.scale, view.scale);
            canvas.clipRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
            onDrawWorld(canvas);
            canvas.restoreToCount(saved);
        } finally {
            if (canvas != null) {
                surfaceHolder.unlockCanvasAndPost(canvas);
                renderedFrames++;
            }
        }
    }

    protected final void drawSprite(Canvas canvas, Sprite sprite, float x, float y) {
        canvas.drawBitmap(images.get(sprite), null,
                new RectF(x - sprite.width / 2f, y - sprite.height / 2f,
                        x + sprite.width / 2f, y + sprite.height / 2f), paint);
    }

    protected final void drawBackground(Canvas canvas, float offset) {
        canvas.drawBitmap(images.background(), null, new RectF(0, offset - 900, 600, offset), paint);
        canvas.drawBitmap(images.background(), null, new RectF(0, offset, 600, offset + 900), paint);
    }

    @Override public final boolean onTouchEvent(MotionEvent event) {
        Viewport view = viewport;
        if (view == null) return false;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!view.contains(event.getX(), event.getY())) return false;
                pointerId = event.getPointerId(0);
                gestureId++;
                downX = event.getX(); downY = event.getY();
                publishTouch(view, downX, downY);
                return true;
            case MotionEvent.ACTION_MOVE:
                int index = event.findPointerIndex(pointerId);
                if (index >= 0) publishTouch(view, event.getX(index), event.getY(index));
                else clearTouch();
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                if (event.getPointerId(event.getActionIndex()) == pointerId) clearTouch();
                return true;
            case MotionEvent.ACTION_UP:
                clearTouch(); performClick(); return true;
            case MotionEvent.ACTION_CANCEL:
                clearTouch(); return true;
            default: return true;
        }
    }

    private void publishTouch(Viewport view, float x, float y) {
        touch = new TouchState(true, gestureId, view.version, downX, downY, x, y);
    }
    private void clearTouch() { pointerId = -1; touch = TouchState.NONE; }
    @Override public boolean performClick() { super.performClick(); return true; }
    public static int liveLoopCount() { return LIVE_LOOPS.get(); }
    public final Viewport currentViewport() { return viewport; }
    public final long renderedFrameCount() { return renderedFrames; }
    protected abstract void onTick(TouchState input, Viewport view);
    protected abstract void onDrawWorld(Canvas canvas);
}

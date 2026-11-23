package edu.hitsz.test;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import edu.hitsz.application.BaseGame;
import edu.hitsz.application.EasyGame;
import edu.hitsz.application.MainActivity;
import edu.hitsz.application.Viewport;
import java.util.function.BooleanSupplier;

/** No AndroidX downloads required. Checks the real SurfaceView on a device or emulator. */
public final class GameInstrumentation extends Instrumentation {
    private MainActivity activity;
    private long downTime;
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void await(BooleanSupplier condition, String message) {
        long deadline = SystemClock.uptimeMillis() + 5000;
        while (!condition.getAsBoolean() && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(40);
        check(condition.getAsBoolean(), message);
    }
    private void touch(EasyGame game, int action, float worldX, float worldY) {
        runOnMainSync(() -> {
            Viewport viewport = game.currentViewport();
            long now = SystemClock.uptimeMillis();
            if (action == MotionEvent.ACTION_DOWN) downTime = now;
            MotionEvent event = MotionEvent.obtain(downTime, now, action,
                    viewport.offsetX + worldX * viewport.scale, viewport.offsetY + worldY * viewport.scale, 0);
            try { game.dispatchTouchEvent(event); } finally { event.recycle(); }
        });
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName("edu.hitsz.aircraftwar", MainActivity.class.getName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = (MainActivity) startActivitySync(intent);
            EasyGame game = activity.gameView();
            await(() -> BaseGame.liveLoopCount() == 1 && game.snapshot().ticks > 0, "Surface loop did not start");
            EasyGame.Snapshot before = game.snapshot();
            touch(game, MotionEvent.ACTION_DOWN, before.heroX, before.heroY);
            SystemClock.sleep(80);
            touch(game, MotionEvent.ACTION_MOVE, 220, 450);
            await(() -> game.snapshot().heroX == 220 && game.snapshot().heroY == 450, "Touch coordinates disagree");
            touch(game, MotionEvent.ACTION_CANCEL, 220, 450);
            touch(game, MotionEvent.ACTION_MOVE, 400, 600);
            SystemClock.sleep(120);
            check(game.snapshot().heroX == 220, "Cancelled pointer moved the hero");
            await(() -> game.snapshot().bullets > 0, "Hero did not shoot");
            await(() -> game.snapshot().enemies > 0, "Enemies did not spawn");
            for (int i = 0; i < 5; i++) {
                runOnMainSync(() -> activity.moveTaskToBack(true));
                await(() -> BaseGame.liveLoopCount() == 0, "Background left a live worker");
                int pausedTicks = game.snapshot().ticks;
                SystemClock.sleep(160);
                check(game.snapshot().ticks == pausedTicks, "World advanced in background");
                Intent resume = new Intent().setClassName("edu.hitsz.aircraftwar", MainActivity.class.getName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                runOnMainSync(() -> getTargetContext().startActivity(resume));
                await(() -> BaseGame.liveLoopCount() == 1 && game.snapshot().ticks > pausedTicks, "Resume failed");
            }
            runOnMainSync(() -> activity.finish());
            await(() -> BaseGame.liveLoopCount() == 0, "Finishing left a worker");
            result.putString("stream", "RESULT: PASS — startup, touch, cancel, shooting, spawning, 5 pause/resume cycles, exit\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable failure) {
            if (activity != null) runOnMainSync(() -> activity.finish());
            result.putString("stream", "RESULT: FAIL — " + failure + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}

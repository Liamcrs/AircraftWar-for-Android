package edu.hitsz.test;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.MotionEvent;
import edu.hitsz.application.BaseGame;
import edu.hitsz.application.EasyGame;
import edu.hitsz.application.MainActivity;
import edu.hitsz.application.Viewport;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** Real device checks. Foreground launches originate from the test shell, like a launcher tap. */
public final class GameInstrumentation extends Instrumentation {
    private final AtomicReference<MainActivity> foreground = new AtomicReference<>();
    private final StringBuilder progress = new StringBuilder();
    private MainActivity activity;
    private long downTime;
    private final Application.ActivityLifecycleCallbacks lifecycle = new Application.ActivityLifecycleCallbacks() {
        @Override public void onActivityResumed(Activity value) {
            if (value instanceof MainActivity) foreground.set((MainActivity) value);
        }
        @Override public void onActivityPaused(Activity value) {
            if (value instanceof MainActivity) foreground.compareAndSet((MainActivity) value, null);
        }
        @Override public void onActivityCreated(Activity value, Bundle state) { }
        @Override public void onActivityStarted(Activity value) { }
        @Override public void onActivityStopped(Activity value) { }
        @Override public void onActivitySaveInstanceState(Activity value, Bundle state) { }
        @Override public void onActivityDestroyed(Activity value) { }
    };
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
    private String shell(String command) throws Exception {
        ParcelFileDescriptor descriptor = getUiAutomation().executeShellCommand(command);
        try (InputStream input = new ParcelFileDescriptor.AutoCloseInputStream(descriptor);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] bytes = new byte[4096];
            int size;
            while ((size = input.read(bytes)) != -1) output.write(bytes, 0, size);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @Override public void onStart() {
        Bundle result = new Bundle();
        int resultCode = Activity.RESULT_CANCELED;
        Application application = (Application) getTargetContext().getApplicationContext();
        runOnMainSync(() -> application.registerActivityLifecycleCallbacks(lifecycle));
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN).setClassName("edu.hitsz.aircraftwar", MainActivity.class.getName())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity = (MainActivity) startActivitySync(intent);
            EasyGame game = activity.gameView();
            await(() -> BaseGame.liveLoopCount() == 1 && game.snapshot().ticks > 0, "Surface loop did not start");
            progress.append("PASS startup\n");
            EasyGame.Snapshot before = game.snapshot();
            touch(game, MotionEvent.ACTION_DOWN, before.heroX, before.heroY);
            SystemClock.sleep(80);
            touch(game, MotionEvent.ACTION_MOVE, 220, 450);
            await(() -> game.snapshot().heroX == 220 && game.snapshot().heroY == 450, "Touch coordinates disagree");
            touch(game, MotionEvent.ACTION_CANCEL, 220, 450);
            touch(game, MotionEvent.ACTION_MOVE, 400, 600);
            SystemClock.sleep(120);
            check(game.snapshot().heroX == 220, "Cancelled pointer moved the hero");
            progress.append("PASS touch and cancel\n");
            await(() -> game.snapshot().bullets > 0, "Hero did not shoot");
            await(() -> game.snapshot().enemies > 0, "Enemies did not spawn");
            progress.append("PASS shooting and spawning\n");
            for (int i = 0; i < 5; i++) {
                EasyGame pausedGame = activity.gameView();
                runOnMainSync(() -> check(activity.moveTaskToBack(true), "Could not background task"));
                await(() -> BaseGame.liveLoopCount() == 0 && foreground.get() == null, "Background left a live worker");
                int pausedTicks = pausedGame.snapshot().ticks;
                long pausedFrames = pausedGame.renderedFrameCount();
                SystemClock.sleep(160);
                check(pausedGame.snapshot().ticks == pausedTicks, "World advanced in background");
                check(pausedGame.renderedFrameCount() == pausedFrames, "Rendering continued in background");
                progress.append("PAUSED cycle ").append(i + 1).append(" ticks=").append(pausedTicks).append('\n');
                String launch = shell("am start -W --activity-reorder-to-front --activity-single-top -n "
                        + "edu.hitsz.aircraftwar/edu.hitsz.application.MainActivity");
                progress.append(launch).append('\n');
                await(() -> foreground.get() != null, "Foreground launch did not resume an Activity");
                activity = foreground.get(); // May be a fresh instance after OS recreation.
                EasyGame resumedGame = activity.gameView();
                long resumedFrames = resumedGame.renderedFrameCount();
                // A finished world intentionally stops ticks, but its Surface must still render.
                await(() -> BaseGame.liveLoopCount() == 1 && resumedGame.renderedFrameCount() > resumedFrames,
                        "Resume failed: Surface did not render; live=" + BaseGame.liveLoopCount());
                check(BaseGame.liveLoopCount() == 1, "More than one game worker");
                progress.append("PASS resume cycle ").append(i + 1).append('\n');
            }
            runOnMainSync(() -> activity.finish());
            await(() -> BaseGame.liveLoopCount() == 0, "Finishing left a worker");
            progress.append("PASS exit\nRESULT: PASS — startup, touch, cancel, shooting, spawning, 5 pause/resume cycles, exit\n");
            resultCode = Activity.RESULT_OK;
        } catch (Throwable failure) {
            progress.append("RESULT: FAIL — ").append(failure).append('\n');
            progress.append("liveLoops=").append(BaseGame.liveLoopCount())
                    .append(" foreground=").append(foreground.get() != null).append('\n');
        } finally {
            runOnMainSync(() -> {
                if (activity != null && !activity.isFinishing()) activity.finish();
                application.unregisterActivityLifecycleCallbacks(lifecycle);
            });
        }
        result.putString("stream", progress.toString());
        finish(resultCode, result);
    }
}

package edu.hitsz.music;

import android.content.Context;
import android.os.Looper;
import edu.hitsz.aircraftwar.R;
import edu.hitsz.application.GameEvent;
import java.util.List;

/** Main-thread owner of audio for one game Activity. */
public final class GameAudioManager {
    private final MyMediaPlayer normal, boss;
    private final MySoundPool effects;
    private boolean enabled, foreground, bossActive, gameOver, released;

    public GameAudioManager(Context context, boolean enabled) {
        checkMainThread();
        this.enabled = enabled;
        normal = new MyMediaPlayer(context, R.raw.bgm);
        boss = new MyMediaPlayer(context, R.raw.bgm_boss);
        effects = new MySoundPool(context);
    }

    public void setEnabled(boolean enabled) {
        checkMainThread();
        if (released) return;
        this.enabled = enabled;
        if (!enabled) effects.stopAll();
        updateMusic();
    }

    public void setForeground(boolean foreground) {
        checkMainThread();
        if (released) return;
        this.foreground = foreground;
        if (!foreground) effects.stopAll();
        updateMusic();
    }

    public void startRound() {
        checkMainThread();
        if (released) return;
        bossActive = gameOver = false;
        effects.stopAll();
        updateMusic();
    }

    public void updateRound(boolean bossActive, boolean over, List<GameEvent> events) {
        checkMainThread();
        if (released || gameOver) return;
        this.bossActive = bossActive;
        gameOver = over;
        updateMusic();
        if (foreground && enabled) {
            for (GameEvent event : events) effects.play(event);
        }
    }

    private void updateMusic() {
        if (!enabled || !foreground || gameOver) {
            normal.setPlaying(false);
            boss.setPlaying(false);
        } else if (bossActive) {
            normal.setPlaying(false);
            boss.setPlaying(true);
        } else {
            boss.setPlaying(false);
            normal.setPlaying(true);
        }
    }

    public void release() {
        checkMainThread();
        if (released) return;
        released = true;
        normal.release();
        boss.release();
        effects.release();
    }

    private static void checkMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper())
            throw new IllegalStateException("Game audio belongs to the main thread");
    }
}

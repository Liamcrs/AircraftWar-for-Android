package edu.hitsz.application;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import edu.hitsz.aircraftwar.R;
import edu.hitsz.music.GameAudioManager;
import java.util.List;

/** Owns scene lifecycle and presents game results on the main thread. */
public final class GameActivity extends Activity {
    public static final String EXTRA_DIFFICULTY = "difficulty";
    public static final String EXTRA_SOUND_ENABLED = "sound_enabled";
    private GameScene game;
    private Difficulty difficulty;
    private boolean soundEnabled;
    private boolean resumed;
    private GameAudioManager audio;
    private int expectedRound;
    private Integer finalScore;
    private AlertDialog resultDialog;
    private final Handler resultHandler = new Handler(Looper.getMainLooper(), message -> {
        if (message.what != GameScene.MSG_ROUND_UPDATE) return false;
        if (resumed && !isFinishing() && !isDestroyed())
            acceptUpdate((GameScene.RoundUpdate) message.obj, true);
        return true;
    });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        difficulty = Difficulty.fromName(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        soundEnabled = getIntent().getBooleanExtra(EXTRA_SOUND_ENABLED,
                new GameSettings(this).isSoundEnabled());
        audio = new GameAudioManager(this, soundEnabled);
        switch (difficulty) {
            case NORMAL: game = new NormalGame(this, resultHandler); break;
            case HARD: game = new HardGame(this, resultHandler); break;
            default: game = new EasyGame(this, resultHandler);
        }
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff101b27);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        root.addView(game, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        root.requestApplyInsets();
    }

    private void acceptUpdate(GameScene.RoundUpdate update, boolean playEvents) {
        if (update.round != expectedRound) return;
        audio.updateRound(update.boss, update.gameOver, playEvents ? update.events : List.of());
        if (update.gameOver) {
            finalScore = update.score;
            showResultIfReady();
        }
    }

    private void showResultIfReady() {
        if (!resumed || finalScore == null || resultDialog != null || isFinishing() || isDestroyed()) return;
        resultDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.game_over)
                .setMessage(getString(R.string.result_score, finalScore))
                .setPositiveButton(R.string.play_again, (dialog, which) -> {
                    finalScore = null;
                    resultDialog = null;
                    expectedRound++;
                    audio.startRound();
                    game.requestRestart();
                })
                .setNegativeButton(R.string.return_menu, (dialog, which) -> finish())
                .setCancelable(false).create();
        resultDialog.show();
    }

    @Override public void onResume() {
        super.onResume();
        resumed = true;
        audio.setForeground(true);
        game.resumeGame();
        showResultIfReady();
    }
    @Override public void onPause() {
        resumed = false;
        audio.setForeground(false);
        game.pauseGame();
        // No new messages can arrive once the worker is joined. Keep state,
        // but discard transient sounds from the old foreground interval.
        resultHandler.removeMessages(GameScene.MSG_ROUND_UPDATE);
        acceptUpdate(game.roundState(), false);
        super.onPause();
    }
    @Override public void onDestroy() {
        game.pauseGame();
        resultHandler.removeCallbacksAndMessages(null);
        audio.release();
        if (resultDialog != null) resultDialog.dismiss();
        super.onDestroy();
    }
    public GameScene gameView() { return game; }
    public Difficulty difficulty() { return difficulty; }
    public boolean isSoundEnabled() { return soundEnabled; }
}

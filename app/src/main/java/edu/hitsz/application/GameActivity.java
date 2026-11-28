package edu.hitsz.application;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowInsets;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import edu.hitsz.aircraftwar.R;
import edu.hitsz.music.GameAudioManager;
import edu.hitsz.rank.RankRecord;
import edu.hitsz.rank.RankRepository;
import java.util.List;
import java.util.UUID;

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
    private EditText nameInput;
    private String recordId, pendingName = "";
    private long finishedAt;
    private boolean saving, restoredResult;
    private RankRepository ranks;
    private final Handler resultHandler = new Handler(Looper.getMainLooper(), message -> {
        if (message.what != GameScene.MSG_ROUND_UPDATE) return false;
        if (resumed && !isFinishing() && !isDestroyed())
            acceptUpdate((GameScene.RoundUpdate) message.obj, true);
        return true;
    });

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ranks = new RankRepository(this);
        if (state != null && state.containsKey("final_score")) {
            finalScore = state.getInt("final_score");
            recordId = state.getString("record_id");
            finishedAt = state.getLong("finished_at");
            pendingName = state.getString("player_name", "");
            restoredResult = true;
        }
        difficulty = Difficulty.fromName(getIntent().getStringExtra(EXTRA_DIFFICULTY));
        soundEnabled = getIntent().getBooleanExtra(EXTRA_SOUND_ENABLED,
                new GameSettings(this).isSoundEnabled());
        audio = new GameAudioManager(this, soundEnabled);
        if (restoredResult) audio.updateRound(false, true, List.of());
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
            if (finalScore == null) {
                recordId = UUID.randomUUID().toString();
                finishedAt = System.currentTimeMillis();
            }
            finalScore = update.score;
            showResultIfReady();
        }
    }

    private void showResultIfReady() {
        if (!resumed || finalScore == null || resultDialog != null || isFinishing() || isDestroyed()) return;
        nameInput = new EditText(this);
        nameInput.setSingleLine(true);
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        nameInput.setHint(R.string.rank_name_hint);
        nameInput.setContentDescription(getString(R.string.rank_name));
        nameInput.setFilters(new InputFilter[] {new InputFilter.LengthFilter(RankRecord.MAX_NAME_LENGTH)});
        nameInput.setText(pendingName);
        resultDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.game_over)
                .setMessage(getString(R.string.result_score, finalScore))
                .setView(nameInput)
                .setPositiveButton(R.string.rank_save, null)
                .setNeutralButton(R.string.play_again, (dialog, which) -> {
                    if (restoredResult) {
                        startActivity(getIntent());
                        finish();
                        return;
                    }
                    finalScore = null;
                    resultDialog = null;
                    pendingName = nameInput.getText().toString();
                    expectedRound++;
                    audio.startRound();
                    game.requestRestart();
                })
                .setNegativeButton(R.string.return_menu, (dialog, which) -> finish())
                .setCancelable(false).create();
        resultDialog.show();
        resultDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> saveResult());
    }

    private void saveResult() {
        if (saving) return;
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) { nameInput.setError(getString(R.string.rank_name_required)); return; }
        saving = true;
        setResultButtonsEnabled(false);
        RankRecord record = new RankRecord(recordId, name, finalScore, finishedAt);
        ranks.save(record, () -> {
            if (isFinishing() || isDestroyed()) return;
            startActivity(new Intent(this, RankActivity.class));
            finish();
        }, failure -> {
            if (isFinishing() || isDestroyed()) return;
            saving = false;
            setResultButtonsEnabled(true);
            Toast.makeText(this, R.string.rank_save_error, Toast.LENGTH_LONG).show();
        });
    }

    private void setResultButtonsEnabled(boolean enabled) {
        nameInput.setEnabled(enabled);
        for (int button : new int[] {AlertDialog.BUTTON_POSITIVE, AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL})
            resultDialog.getButton(button).setEnabled(enabled);
        resultDialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(enabled ? R.string.rank_save : R.string.rank_saving);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (finalScore != null) {
            state.putInt("final_score", finalScore);
            state.putString("record_id", recordId);
            state.putLong("finished_at", finishedAt);
            state.putString("player_name", nameInput == null ? pendingName : nameInput.getText().toString());
        }
        super.onSaveInstanceState(state);
    }

    @Override public void onResume() {
        super.onResume();
        resumed = true;
        audio.setForeground(true);
        if (!restoredResult) game.resumeGame();
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

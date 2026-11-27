package edu.hitsz.application;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.View;
import edu.hitsz.aircraftwar.R;

/** Native difficulty-selection menu. */
public final class MainActivity extends Activity {
    private boolean launching;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.menu_root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();

        findViewById(R.id.easy_button).setOnClickListener(view -> launch(Difficulty.EASY));
        findViewById(R.id.normal_button).setOnClickListener(view -> launch(Difficulty.NORMAL));
        findViewById(R.id.hard_button).setOnClickListener(view -> launch(Difficulty.HARD));
    }

    private void launch(Difficulty difficulty) {
        if (launching) return;
        launching = true;
        startActivity(new Intent(this, GameActivity.class)
                .putExtra(GameActivity.EXTRA_DIFFICULTY, difficulty.name()));
    }

    @Override protected void onResume() { super.onResume(); launching = false; }
}

package edu.hitsz.application;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.View;
import android.widget.TextView;
import edu.hitsz.aircraftwar.R;

/** Native difficulty-selection menu. */
public final class MainActivity extends Activity {
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

        TextView selection = findViewById(R.id.selected_mode);
        findViewById(R.id.easy_button).setOnClickListener(view ->
                selection.setText(getString(R.string.selected_mode, getString(R.string.easy_mode))));
        findViewById(R.id.normal_button).setOnClickListener(view ->
                selection.setText(getString(R.string.selected_mode, getString(R.string.normal_mode))));
        findViewById(R.id.hard_button).setOnClickListener(view ->
                selection.setText(getString(R.string.selected_mode, getString(R.string.hard_mode))));
    }
}

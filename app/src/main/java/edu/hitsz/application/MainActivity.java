package edu.hitsz.application;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;

/** Stage 1: verify the native Java Activity entry point before adding the game. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView title = new TextView(this);
        title.setText("AircraftWar2026\n实验二 · Android 工程已创建");
        title.setTextColor(0xffeeeeee);
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        setContentView(title);
    }
}

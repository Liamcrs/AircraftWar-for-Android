package edu.hitsz.application;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowInsets;
import android.widget.FrameLayout;

/** Native Java Activity; the SurfaceView owns the game and rendering worker. */
public final class MainActivity extends Activity {
    private EasyGame game;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xff101b27);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        game = new EasyGame(this);
        root.addView(game, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        root.requestApplyInsets();
    }
    @Override public void onResume() { super.onResume(); if (game != null) game.resumeGame(); }
    @Override public void onPause() { if (game != null) game.pauseGame(); super.onPause(); }
    @Override public void onDestroy() { if (game != null) game.pauseGame(); super.onDestroy(); }
    public EasyGame gameView() { return game; }
}

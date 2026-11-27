package edu.hitsz.application;

import android.content.Context;
import android.os.Handler;
import edu.hitsz.aircraftwar.R;

public final class EasyGame extends GameScene {
    public EasyGame(Context context, Handler resultHandler) {
        super(context, Difficulty.EASY, R.drawable.bg_easy, resultHandler);
    }
}

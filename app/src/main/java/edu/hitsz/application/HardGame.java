package edu.hitsz.application;

import android.content.Context;
import android.os.Handler;
import edu.hitsz.aircraftwar.R;

public final class HardGame extends GameScene {
    public HardGame(Context context, Handler resultHandler) {
        super(context, Difficulty.HARD, R.drawable.bg_hard, resultHandler);
    }
}

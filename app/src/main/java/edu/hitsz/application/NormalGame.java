package edu.hitsz.application;

import android.content.Context;
import android.os.Handler;
import edu.hitsz.aircraftwar.R;

public final class NormalGame extends GameScene {
    public NormalGame(Context context, Handler resultHandler) {
        super(context, Difficulty.NORMAL, R.drawable.bg_normal, resultHandler);
    }
}

package edu.hitsz.application;

import android.app.Activity;
import android.os.Bundle;

/** Stage 2: preview the migrated bitmap resources. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(new ResourcePreview(this));
    }
}

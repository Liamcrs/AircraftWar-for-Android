package edu.hitsz.application;

import android.content.Context;
import android.content.SharedPreferences;

/** Persistent player preferences shared by activities. */
public final class GameSettings {
    private static final String PREFERENCES = "game_settings";
    private static final String SOUND_ENABLED = "sound_enabled";
    private final SharedPreferences preferences;

    public GameSettings(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    public boolean isSoundEnabled() {
        return preferences.getBoolean(SOUND_ENABLED, true);
    }

    public void setSoundEnabled(boolean enabled) {
        preferences.edit().putBoolean(SOUND_ENABLED, enabled).apply();
    }
}

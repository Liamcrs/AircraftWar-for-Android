package edu.hitsz.music;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;
import edu.hitsz.aircraftwar.R;
import edu.hitsz.application.GameEvent;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Short effects, asynchronously loaded and dispatched exclusively on the main thread. */
final class MySoundPool {
    private static final String TAG = "AircraftAudio";
    private static final int MAX_TRACKED_STREAMS = 32;
    private final Map<GameEvent, Integer> samples = new EnumMap<>(GameEvent.class);
    private final Set<Integer> loaded = new HashSet<>();
    // SoundPool has no completion callback. Keep a bounded history of successful streams;
    // explicitly stop an evicted ID so every possibly active stream remains tracked.
    private final Map<Integer, GameEvent> activeStreams = new LinkedHashMap<>();
    private SoundPool pool;
    private boolean pendingGameOver;

    MySoundPool(Context context) {
        try {
            pool = new SoundPool.Builder().setMaxStreams(8)
                    .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
            pool.setOnLoadCompleteListener((source, sample, status) -> {
                if (pool != source) return;
                if (status != 0) {
                    Log.e(TAG, "SoundPool load failed for sample " + sample + ": " + status);
                    if (Integer.valueOf(sample).equals(samples.get(GameEvent.GAME_OVER))) pendingGameOver = false;
                    return;
                }
                loaded.add(sample);
                if (pendingGameOver && Integer.valueOf(sample).equals(samples.get(GameEvent.GAME_OVER))) {
                    pendingGameOver = false;
                    play(GameEvent.GAME_OVER);
                }
            });
            Context app = context.getApplicationContext();
            load(app, GameEvent.BULLET_HIT, R.raw.bullet_hit);
            load(app, GameEvent.BOMB_EXPLOSION, R.raw.bomb_explosion);
            load(app, GameEvent.GET_SUPPLY, R.raw.get_supply);
            load(app, GameEvent.GAME_OVER, R.raw.game_over);
        } catch (RuntimeException failure) {
            Log.e(TAG, "Cannot initialize SoundPool", failure);
            release();
        }
    }

    private void load(Context context, GameEvent event, int resource) {
        int id = pool.load(context, resource, 1);
        if (id == 0) Log.e(TAG, "Cannot load sound " + event);
        else samples.put(event, id);
    }

    void play(GameEvent event) {
        if (pool == null) return;
        Integer id = samples.get(event);
        if (id == null) return;
        if (!loaded.contains(id)) {
            // Only game over may wait for loading. Never replay stale collisions/pickups.
            if (event == GameEvent.GAME_OVER) pendingGameOver = true;
            return;
        }
        if (activeStreams.size() >= MAX_TRACKED_STREAMS) {
            int oldest = activeStreams.keySet().iterator().next();
            pool.stop(oldest);
            activeStreams.remove(oldest);
        }
        int stream = pool.play(id, 1f, 1f, 1, 0, 1f);
        if (stream != 0) {
            activeStreams.put(stream, event);
            Log.d(TAG, "Played " + event + " stream=" + stream);
        } else Log.w(TAG, "SoundPool could not play " + event);
    }

    void stopAll() {
        pendingGameOver = false;
        if (pool != null) for (int stream : activeStreams.keySet()) pool.stop(stream);
        activeStreams.clear();
    }

    void release() {
        stopAll();
        if (pool != null) {
            pool.setOnLoadCompleteListener(null);
            pool.release();
            pool = null;
        }
        loaded.clear();
        samples.clear();
    }
}

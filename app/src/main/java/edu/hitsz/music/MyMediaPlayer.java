package edu.hitsz.music;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;
import java.io.IOException;

/** A looping track with asynchronous preparation; all calls occur on the main thread. */
final class MyMediaPlayer {
    private static final String TAG = "AircraftAudio";
    private final int resource;
    private MediaPlayer player;
    private boolean prepared, desiredPlaying, started, released;

    MyMediaPlayer(Context context, int resource) {
        this.resource = resource;
        try (AssetFileDescriptor fd = context.getApplicationContext().getResources().openRawResourceFd(resource)) {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            player.setOnPreparedListener(ready -> {
                if (released || player != ready) return;
                prepared = true;
                applyDesiredState();
            });
            player.setOnErrorListener((failed, what, extra) -> {
                if (player == failed && !released) {
                    Log.e(TAG, "BGM resource " + resource + " failed: " + what + "/" + extra);
                    release();
                }
                return true;
            });
            player.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            player.setLooping(true);
            player.prepareAsync();
        } catch (IOException | RuntimeException failure) {
            Log.e(TAG, "Cannot prepare BGM resource " + resource, failure);
            release();
        }
    }

    void setPlaying(boolean playing) {
        if (released) return;
        desiredPlaying = playing;
        applyDesiredState();
    }

    private void applyDesiredState() {
        if (!prepared || released || started == desiredPlaying) return;
        try {
            if (desiredPlaying) player.start();
            else player.pause();
            started = desiredPlaying;
        } catch (IllegalStateException failure) {
            Log.e(TAG, "Cannot change BGM playback for " + resource, failure);
            release();
        }
    }

    void release() {
        if (released) return;
        released = true;
        prepared = desiredPlaying = started = false;
        if (player != null) {
            player.setOnPreparedListener(null);
            player.setOnErrorListener(null);
            player.release();
            player = null;
        }
    }
}

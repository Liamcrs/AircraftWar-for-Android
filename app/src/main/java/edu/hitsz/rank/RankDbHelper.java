package edu.hitsz.rank;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

/** Versioned schema for app-private leaderboard storage. */
public final class RankDbHelper extends SQLiteOpenHelper {
    public RankDbHelper(Context context) {
        super(context.getApplicationContext(), "rank.db", null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE rank ("
                + "_id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL,"
                + "score INTEGER NOT NULL CHECK(score >= 0),"
                + "created_at INTEGER NOT NULL CHECK(created_at >= 0))");
        db.execSQL("CREATE INDEX rank_order ON rank(score DESC, created_at ASC, _id ASC)");
        db.execSQL("CREATE TABLE rank_meta (key TEXT PRIMARY KEY NOT NULL, value TEXT NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Future versions must provide a migration; never drop a player's history.
        throw new SQLiteException("Unsupported rank schema upgrade: " + oldVersion + " -> " + newVersion);
    }
}

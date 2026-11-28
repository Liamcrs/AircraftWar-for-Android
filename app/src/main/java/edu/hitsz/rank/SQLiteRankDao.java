package edu.hitsz.rank;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import java.util.ArrayList;
import java.util.List;

/** SQLite implementation of the same contract used by FileRankDao. */
public final class SQLiteRankDao implements RankDao {
    private static final String FILE_MIGRATION = "file_rank_v1";
    private final RankDbHelper helper;
    private final SQLiteDatabase db;

    public SQLiteRankDao(Context context) {
        helper = new RankDbHelper(context);
        try {
            db = helper.getWritableDatabase();
            migrateFile(context);
        } catch (RuntimeException failure) {
            helper.close();
            throw new RankStorageException("Cannot open leaderboard database", failure);
        }
    }

    @Override public List<RankRecord> findAll() {
        List<RankRecord> records = new ArrayList<>();
        try (Cursor cursor = db.query("rank", new String[] {"_id", "name", "score", "created_at"},
                null, null, null, null, "score DESC, created_at ASC, _id ASC")) {
            while (cursor.moveToNext()) records.add(new RankRecord(cursor.getString(0),
                    cursor.getString(1), cursor.getInt(2), cursor.getLong(3)));
            return records;
        } catch (RuntimeException failure) {
            throw new RankStorageException("Cannot read leaderboard database", failure);
        }
    }

    @Override public void insert(RankRecord record) {
        try { insertRecord(record); }
        catch (SQLiteException failure) {
            throw new RankStorageException("Cannot save leaderboard record", failure);
        }
    }

    private void insertRecord(RankRecord record) {
        ContentValues values = new ContentValues();
        values.put("_id", record.id);
        values.put("name", record.name);
        values.put("score", record.score);
        values.put("created_at", record.createdAt);
        long result = db.insertWithOnConflict("rank", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        if (result == -1) {
            try (Cursor existing = db.query("rank", new String[] {"_id"}, "_id=?",
                    new String[] {record.id}, null, null, null)) {
                if (!existing.moveToFirst()) throw new SQLiteException("Rank insertion failed");
            }
        }
    }

    @Override public void delete(String id) {
        try { db.delete("rank", "_id=?", new String[] {id}); }
        catch (SQLiteException failure) {
            throw new RankStorageException("Cannot delete leaderboard record", failure);
        }
    }

    private void migrateFile(Context context) {
        db.beginTransaction();
        try {
            boolean migrated;
            try (Cursor cursor = db.query("rank_meta", new String[] {"value"}, "key=?",
                    new String[] {FILE_MIGRATION}, null, null, null)) {
                migrated = cursor.moveToFirst();
            }
            if (!migrated) {
                for (RankRecord record : new FileRankDao(context).findAll()) insertRecord(record);
                ContentValues marker = new ContentValues();
                marker.put("key", FILE_MIGRATION);
                marker.put("value", "complete");
                db.insertOrThrow("rank_meta", null, marker);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override public void close() { helper.close(); }
}

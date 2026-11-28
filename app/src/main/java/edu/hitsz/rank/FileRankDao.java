package edu.hitsz.rank;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** App-private JSON storage. AtomicFile preserves the previous file on write failure. */
public final class FileRankDao implements RankDao {
    private final AtomicFile file;
    public FileRankDao(Context context) {
        file = new AtomicFile(new File(context.getApplicationContext().getFilesDir(), "rank.json"));
    }

    @Override public List<RankRecord> findAll() {
        List<RankRecord> records = new ArrayList<>();
        if (!file.getBaseFile().exists() && !new File(file.getBaseFile().getPath() + ".bak").exists()) return records;
        try {
            JSONArray array = new JSONArray(new String(file.readFully(), StandardCharsets.UTF_8));
            for (int i = 0; i < array.length(); i++) {
                JSONObject row = array.getJSONObject(i);
                records.add(new RankRecord(row.getString("id"), row.getString("name"),
                        row.getInt("score"), row.getLong("createdAt")));
            }
            records.sort(RankRecord.ORDER);
            return records;
        } catch (IOException | JSONException | IllegalArgumentException failure) {
            throw new RankStorageException("Cannot read rank file", failure);
        }
    }

    @Override public void insert(RankRecord record) {
        List<RankRecord> records = findAll();
        for (RankRecord existing : records) if (existing.id.equals(record.id)) return;
        records.add(record);
        write(records);
    }

    private void write(List<RankRecord> records) {
        FileOutputStream stream = null;
        try {
            JSONArray array = new JSONArray();
            for (RankRecord record : records) {
                JSONObject row = new JSONObject();
                row.put("id", record.id);
                row.put("name", record.name);
                row.put("score", record.score);
                row.put("createdAt", record.createdAt);
                array.put(row);
            }
            stream = file.startWrite();
            stream.write(array.toString().getBytes(StandardCharsets.UTF_8));
            file.finishWrite(stream);
        } catch (IOException | JSONException failure) {
            if (stream != null) file.failWrite(stream);
            throw new RankStorageException("Cannot save rank file", failure);
        }
    }
}

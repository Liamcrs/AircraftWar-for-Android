package edu.hitsz.rank;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/** Serializes disk access off the UI thread, delivering results to the main thread. */
public final class RankRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Context context;

    public RankRepository(Context context) { this.context = context.getApplicationContext(); }

    public void load(Consumer<List<RankRecord>> success, Consumer<RuntimeException> failure) {
        submit(RankDao::findAll, success, failure);
    }

    public void save(RankRecord record, Runnable success, Consumer<RuntimeException> failure) {
        submit(dao -> { dao.insert(record); return true; }, ignored -> success.run(), failure);
    }

    public void delete(String id, Consumer<List<RankRecord>> success, Consumer<RuntimeException> failure) {
        submit(dao -> { dao.delete(id); return dao.findAll(); }, success, failure);
    }

    private RankDao openDao() { return new FileRankDao(context); }

    private <T> void submit(Function<RankDao, T> operation, Consumer<T> success,
                            Consumer<RuntimeException> failure) {
        IO.execute(() -> {
            try (RankDao dao = openDao()) {
                T result = operation.apply(dao);
                main.post(() -> success.accept(result));
            } catch (RuntimeException error) {
                Log.e("RankStorage", "Leaderboard operation failed", error);
                main.post(() -> failure.accept(error));
            }
        });
    }
}

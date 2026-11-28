package edu.hitsz.application;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import edu.hitsz.aircraftwar.R;
import edu.hitsz.rank.RankAdapter;
import edu.hitsz.rank.RankRepository;
import edu.hitsz.rank.RankRecord;

/** Controller for the leaderboard page. */
public final class RankActivity extends Activity {
    private RankAdapter adapter;
    private RankRepository repository;
    private AlertDialog deleteDialog;
    private boolean deleting;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_rank);
        View root = findViewById(R.id.rank_root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsets.CONSUMED;
        });
        root.requestApplyInsets();
        ListView list = findViewById(R.id.rank_list);
        adapter = new RankAdapter(this);
        repository = new RankRepository(this);
        adapter.setOnDeleteListener(this::confirmDelete);
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.rank_empty));
        findViewById(R.id.rank_back).setOnClickListener(view -> finish());
    }

    @Override protected void onResume() {
        super.onResume();
        loadRecords();
    }

    private void loadRecords() {
        TextView empty = findViewById(R.id.rank_empty);
        empty.setText(R.string.rank_loading);
        repository.load(records -> {
            if (isFinishing() || isDestroyed()) return;
            empty.setText(R.string.rank_empty);
            empty.setOnClickListener(null);
            adapter.submitList(records);
        }, failure -> {
            if (isFinishing() || isDestroyed()) return;
            empty.setText(R.string.rank_load_error);
            empty.setOnClickListener(view -> loadRecords());
            Toast.makeText(this, R.string.rank_load_error, Toast.LENGTH_LONG).show();
        });
    }

    private void confirmDelete(RankRecord record) {
        if (deleting || deleteDialog != null) return;
        deleteDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rank_delete_title)
                .setMessage(getString(R.string.rank_delete_message, record.name, record.score))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.rank_delete, (dialog, which) -> {
                    deleting = true;
                    repository.delete(record.id, records -> {
                        if (isFinishing() || isDestroyed()) return;
                        deleting = false;
                        adapter.submitList(records);
                    }, failure -> {
                        if (isFinishing() || isDestroyed()) return;
                        deleting = false;
                        Toast.makeText(this, R.string.rank_delete_error, Toast.LENGTH_LONG).show();
                    });
                }).create();
        deleteDialog.setOnDismissListener(dialog -> deleteDialog = null);
        deleteDialog.show();
    }

    @Override protected void onDestroy() {
        if (deleteDialog != null) deleteDialog.dismiss();
        super.onDestroy();
    }
}

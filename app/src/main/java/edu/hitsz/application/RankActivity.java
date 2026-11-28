package edu.hitsz.application;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ListView;
import edu.hitsz.aircraftwar.R;
import edu.hitsz.rank.RankAdapter;

/** Controller for the leaderboard page. */
public final class RankActivity extends Activity {
    private RankAdapter adapter;

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
        list.setAdapter(adapter);
        list.setEmptyView(findViewById(R.id.rank_empty));
        findViewById(R.id.rank_back).setOnClickListener(view -> finish());
    }
}

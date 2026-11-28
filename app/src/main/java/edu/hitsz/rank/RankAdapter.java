package edu.hitsz.rank;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import edu.hitsz.aircraftwar.R;
import java.util.ArrayList;
import java.util.List;

/** Reuses row views; storage will be supplied by the page controller. */
public final class RankAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final List<RankRecord> rows = new ArrayList<>();

    public RankAdapter(Context context) { inflater = LayoutInflater.from(context); }
    @Override public int getCount() { return rows.size(); }
    public void submitList(List<RankRecord> records) {
        rows.clear();
        rows.addAll(records);
        notifyDataSetChanged();
    }
    @Override public RankRecord getItem(int position) { return rows.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_rank, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else holder = (ViewHolder) convertView.getTag();
        RankRecord row = getItem(position);
        holder.number.setText(String.valueOf(position + 1));
        holder.name.setText(row.name);
        holder.score.setText(String.valueOf(row.score));
        convertView.setBackgroundColor(position == 0 ? 0xfffff1c9 : 0xfff5f7fb);
        return convertView;
    }

    private static final class ViewHolder {
        final TextView number, name, score;
        ViewHolder(View view) {
            number = view.findViewById(R.id.item_rank_number);
            name = view.findViewById(R.id.item_rank_name);
            score = view.findViewById(R.id.item_rank_score);
        }
    }
}

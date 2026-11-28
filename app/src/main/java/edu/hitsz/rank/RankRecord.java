package edu.hitsz.rank;

import java.util.Comparator;

/** One finished round; player names are deliberately not unique. */
public final class RankRecord {
    public static final int MAX_NAME_LENGTH = 24;
    public static final Comparator<RankRecord> ORDER = Comparator
            .comparingInt((RankRecord record) -> record.score).reversed()
            .thenComparingLong(record -> record.createdAt).thenComparing(record -> record.id);
    public final String id, name;
    public final int score;
    public final long createdAt;

    public RankRecord(String id, String name, int score, long createdAt) {
        if (id == null || id.isEmpty() || name == null || name.trim().isEmpty()
                || name.trim().length() > MAX_NAME_LENGTH || score < 0 || createdAt < 0)
            throw new IllegalArgumentException("Invalid rank record");
        this.id = id;
        this.name = name.trim();
        this.score = score;
        this.createdAt = createdAt;
    }
}

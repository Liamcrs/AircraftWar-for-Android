package edu.hitsz.rank;

import java.util.List;

/** UI-independent contract shared by file and database storage. */
public interface RankDao extends AutoCloseable {
    List<RankRecord> findAll();
    /** Retrying an already saved round ID must not add a second row. */
    void insert(RankRecord record);
    @Override default void close() { }
}

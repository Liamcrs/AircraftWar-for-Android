package edu.hitsz.rank;

/** Storage failures must reach the controller instead of pretending the list is empty. */
public final class RankStorageException extends RuntimeException {
    public RankStorageException(String message, Throwable cause) { super(message, cause); }
}

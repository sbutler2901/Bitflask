package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Manages Raft's log of entries. */
// TODO: implement persisting log
@Singleton
final class RaftLog {

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  private static final Entry EMPTY_LOG_ENTRY_SENTINEL =
      Entry.newBuilder()
          .setTerm(0)
          .setEmptyCommand(Entry.EmptyCommand.getDefaultInstance())
          .build();

  @Inject
  RaftLog() {
    entries.add(EMPTY_LOG_ENTRY_SENTINEL);
  }

  /**
   * Inserts the provided {@link Entry}s after the entry provided by {@code prevLogEntryDetails}.
   *
   * <p>If a conflicting Entry is found in the log, that entry and all subsequent ones will be
   * deleted. The rest of the log will be populated with the remaining Entrys in {@code newEntries}
   * including the one that conflicted with the pre-existing Entry.
   *
   * @return true if an {@link Entry} existed in the log matching {@code prevLogEntryDetails}, false
   *     otherwise.
   */
  boolean appendEntriesAfterPrevEntry(LogEntryDetails prevLogEntryDetails, List<Entry> newEntries) {
    if (!logHasMatchingEntry(prevLogEntryDetails)) {
      return false;
    }

    int insertIdx = 1 + prevLogEntryDetails.index();
    for (var newEntry : newEntries) {
      if (insertIdx < entries.size()) {
        if (entries.get(insertIdx).getTerm() != newEntry.getTerm()) {
          // Conflict detected, clear all entries from conflict on
          deleteEntriesFromIndex(insertIdx);
          appendEntry(newEntry);
        }
      } else {
        appendEntry(newEntry);
      }
      insertIdx++;
    }
    return true;
  }

  /** Deletes all {@link Entry}s from {@code deleteStartIdx} through the end of the log. */
  private void deleteEntriesFromIndex(int deleteStartIdx) {
    entries.subList(deleteStartIdx, entries.size()).clear();
  }

  /** Returns true if the log has an {@link Entry} matching the provided {@link LogEntryDetails}. */
  private boolean logHasMatchingEntry(LogEntryDetails logEntryDetails) {
    if (LogEntryDetails.isEmptyLogSentinel(logEntryDetails)) {
      return entries.isEmpty();
    }
    if (logEntryDetails.index() >= entries.size()) {
      return false;
    }
    Entry entry = entries.get(logEntryDetails.index());
    return entry.getTerm() == logEntryDetails.term();
  }

  /** Appends the entry to the log returning its index. */
  int appendEntry(Entry newEntry) {
    entries.add(newEntry);
    return getLastEntryIndex();
  }

  /** The index of the last added {@link Entry}. */
  private int getLastEntryIndex() {
    return entries.size() - 1;
  }

  /** The index to be used for the next {@link Entry} added. */
  private int getNextEntryIndex() {
    return entries.size();
  }

  /** Returns {@link LogEntryDetails} about the last entry in the log. */
  LogEntryDetails getLastLogEntryDetails() {
    return getLogEntryDetails(getLastEntryIndex());
  }

  /** Returns {@link LogEntryDetails} for the {@link Entry} at the provided index. */
  LogEntryDetails getLogEntryDetails(int entryIndex) {
    Preconditions.checkArgument(entryIndex >= 0, "Negative entryIndex provided.");
    return new LogEntryDetails(getEntryAtIndex(entryIndex).getTerm(), entryIndex);
  }

  /** Returns the {@link Entry} at the provided index. */
  Entry getEntryAtIndex(int entryIndex) {
    Preconditions.checkArgument(entryIndex >= 0, "Negative entryIndex provided.");
    return entries.get(entryIndex);
  }

  /** Returns a list of {@link Entry}s starting from the provided index to the last one. */
  ImmutableList<Entry> getEntriesFromIndex(int fromIndex) {
    Preconditions.checkArgument(fromIndex >= 0, "Negative entryIndex provided.");
    return ImmutableList.copyOf(entries.subList(fromIndex, getNextEntryIndex()));
  }

  /**
   * Returns a list of {@link Entry}s from {@code fromIndex} (inclusive) to {@code toIndex}
   * (exclusive).
   *
   * <p>If {@code fromIndex} and greater than the length of the log, all entries up to and including
   * the last will be provided.
   */
  ImmutableList<Entry> getEntriesFromIndex(int fromIndex, int toIndex) {
    Preconditions.checkArgument(fromIndex >= 0, "Negative entryIndex provided.");
    return ImmutableList.copyOf(entries.subList(fromIndex, Math.min(toIndex, getNextEntryIndex())));
  }

  /** Simplified details about an {@link Entry} in the {@link RaftLog}. */
  record LogEntryDetails(int term, int index) implements Comparable<LogEntryDetails> {

    static LogEntryDetails EMPTY_LOG_SENTINEL = new LogEntryDetails(0, 0);

    static boolean isEmptyLogSentinel(LogEntryDetails logEntryDetails) {
      return EMPTY_LOG_SENTINEL.equals(logEntryDetails);
    }

    @Override
    public int compareTo(LogEntryDetails provided) {
      var termsCompared = Integer.compare(term(), provided.term());
      var indexesCompared = Integer.compare(index(), provided.index());

      if (termsCompared > 0) {
        return 1;
      }
      if (termsCompared < 0) {
        return -1;
      }
      return Integer.compare(indexesCompared, 0);
    }
  }
}

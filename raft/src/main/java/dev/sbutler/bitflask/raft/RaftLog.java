package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Manages Raft's log of entries. */
@Singleton
final class RaftLog {

  private final RaftVolatileState raftVolatileState;

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

  @Inject
  RaftLog(RaftVolatileState raftVolatileState) {
    this.raftVolatileState = raftVolatileState;
  }

  boolean appendEntriesFromLeader(
      LogEntryDetails prevLogEntryDetails, List<Entry> newEntries, int leaderCommitIndex) {
    if (!logHasMatchingEntry(prevLogEntryDetails)) {
      return false;
    }
    // TODO: Handled appending entries, handling conflicts, and potentially applying
    if (leaderCommitIndex > raftVolatileState.getHighestCommittedEntryIndex()) {
      raftVolatileState.setHighestCommittedEntryIndex(
          Math.min(leaderCommitIndex, getLastEntryIndex()));
    }
    return true;
  }

  /** Returns true if the log has an {@link Entry} matching the provided {@link LogEntryDetails}. */
  private boolean logHasMatchingEntry(LogEntryDetails logEntryDetails) {
    if (logEntryDetails.index() >= entries.size()) {
      return false;
    }
    Entry entry = entries.get(logEntryDetails.index());
    return entry.getTerm() == logEntryDetails.term();
  }

  /** Appends the entry returning its index. */
  int appendEntry(Entry newEntry) {
    entries.add(newEntry);
    return entries.lastIndexOf(newEntry);
  }

  int getLastEntryIndex() {
    return entries.size() - 1;
  }

  /** Returns {@link LogEntryDetails} about the last entry in the log. */
  LogEntryDetails getLastLogEntryDetails() {
    int lastEntryIndex = getLastEntryIndex();
    Entry lastEntry = entries.get(lastEntryIndex);
    return new LogEntryDetails(lastEntry.getTerm(), lastEntryIndex);
  }

  /** Simplified details about an {@link Entry} in the {@link RaftLog}. */
  record LogEntryDetails(int term, int index) implements Comparable<LogEntryDetails> {

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

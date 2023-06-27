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

  void appendEntriesWithLeaderCommit(List<Entry> newEntries, int leaderCommitIndex) {
    // TODO: Handled appending entries, handling conflicts, and potentially applying
    if (leaderCommitIndex > raftVolatileState.getHighestCommittedEntryIndex()) {
      raftVolatileState.setHighestCommittedEntryIndex(
          Math.min(leaderCommitIndex, getLastEntryIndex()));
    }
  }

  /**
   * Returns true if an entry exists at the provided index with a term matching the provided one.
   */
  boolean logAtIndexHasMatchingTerm(int index, int term) {
    if (index >= entries.size()) {
      return false;
    }
    Entry entry = entries.get(index);
    return entry.getTerm() == term;
  }

  private int getLastEntryIndex() {
    return entries.size() - 1;
  }

  LastLogEntryDetails getLastLogDetails() {
    int lastEntryIndex = getLastEntryIndex();
    Entry lastEntry = entries.get(lastEntryIndex);
    return new LastLogEntryDetails(lastEntry.getTerm(), lastEntryIndex);
  }

  /** Simplified details about the last {@link Entry} in the {@link RaftLog}. */
  record LastLogEntryDetails(int term, int index) implements Comparable<LastLogEntryDetails> {

    @Override
    public int compareTo(LastLogEntryDetails provided) {
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

package dev.sbutler.bitflask.raft;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Manages Raft's log of entries. */
final class RaftLog {

  private final List<Entry> entries = new CopyOnWriteArrayList<>();

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

  LastLogEntryDetails getLastLogDetails() {
    int lastEntryIndex = entries.size() - 1;
    Entry lastEntry = entries.get(lastEntryIndex);
    return new LastLogEntryDetails(lastEntry.getTerm(), lastEntryIndex);
  }

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

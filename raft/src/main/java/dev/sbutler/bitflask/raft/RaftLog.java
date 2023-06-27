package dev.sbutler.bitflask.raft;

/** Manages Raft's log of entries. */
final class RaftLog {

  LastLogDetails getLastLogDetails() {
    return new LastLogDetails(0, 0);
  }

  record LastLogDetails(int term, int index) implements Comparable<LastLogDetails> {

    @Override
    public int compareTo(LastLogDetails provided) {
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

package dev.sbutler.bitflask.raft;

/** Manages Raft's log of entries. */
final class RaftLog {

  LastLogDetails getLastLogDetails() {
    return new LastLogDetails(0, 0);
  }

  record LastLogDetails(long index, long term) {}
}

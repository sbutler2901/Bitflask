package dev.sbutler.bitflask.raft;

import jakarta.inject.Singleton;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Volatile state that must be reinitialized each time a Raft server boots, unless it is the first
 * boot.
 */
@Singleton
final class RaftVolatileState {

  /** Index of highest log entry known to be committed. */
  private final AtomicLong highestCommittedEntryIndex = new AtomicLong(0);
  /** Index of highest log entry applied to state machine. */
  private final AtomicLong highestAppliedEntryIndex = new AtomicLong(0);

  RaftVolatileState(long highestCommittedEntryIndex, long highestAppliedEntryIndex) {
    this.highestCommittedEntryIndex.set(highestCommittedEntryIndex);
    this.highestAppliedEntryIndex.set(highestAppliedEntryIndex);
  }

  /** Returns the index of the highest log entry known to be committed. */
  long getHighestCommittedEntryIndex() {
    return highestCommittedEntryIndex.get();
  }

  /** Sets the index of the highest log entry known to be committed. */
  long setHighestCommittedEntryIndex(long index) {
    return highestCommittedEntryIndex.getAndSet(index);
  }

  /** Returns the index of the highest log entry applied to the state machine. */
  long getHighestAppliedEntryIndex() {
    return highestAppliedEntryIndex.get();
  }

  /** Sets the index of the highest log entry applied to the state machine. */
  long setHighestAppliedEntryIndex(long index) {
    return highestAppliedEntryIndex.getAndSet(index);
  }
}

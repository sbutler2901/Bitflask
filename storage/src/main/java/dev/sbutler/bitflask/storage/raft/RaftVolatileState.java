package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Volatile state that must be reinitialized each time a Raft server boots, unless it is the first
 * boot.
 */
@Singleton
final class RaftVolatileState {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  /** Index of highest log entry known to be committed. */
  private final AtomicInteger highestCommittedEntryIndex = new AtomicInteger(0);

  /** Index of highest log entry applied to state machine. */
  private final AtomicInteger highestAppliedEntryIndex = new AtomicInteger(0);

  private volatile RaftServerId leaderServerId;

  @Inject
  RaftVolatileState() {}

  /** Used to initialize state at startup. */
  void initialize(int highestCommittedEntryIndex, int highestAppliedEntryIndex) {
    increaseHighestCommittedEntryIndexTo(highestCommittedEntryIndex);
    increaseHighestAppliedEntryIndexTo(highestAppliedEntryIndex);
  }

  /** Returns the index of the highest log entry known to be committed. */
  int getHighestCommittedEntryIndex() {
    return highestCommittedEntryIndex.get();
  }

  /** Sets the index of the highest log entry known to be committed. */
  void increaseHighestCommittedEntryIndexTo(int index) {
    Preconditions.checkArgument(
        index >= getHighestCommittedEntryIndex(),
        String.format(
            "Attempting to set committed entry index [%d] lower than current value [%d]",
            index, getHighestCommittedEntryIndex()));
    Preconditions.checkArgument(
        index >= getHighestAppliedEntryIndex(),
        String.format(
            "Attempting to set committed entry index [%d] lower than applied entry index [%d]",
            index, getHighestAppliedEntryIndex()));
    int prev = highestCommittedEntryIndex.getAndSet(index);
    logger.atInfo().log("Set committed entry index to [%d], previous value [%d].", index, prev);
  }

  /** Returns the index of the highest log entry applied to the state machine. */
  int getHighestAppliedEntryIndex() {
    return highestAppliedEntryIndex.get();
  }

  /**
   * Sets the index of the highest log entry applied to the state machine.
   *
   * <p>The applied index cannot be decreased, or set higher than the committed index.
   */
  void increaseHighestAppliedEntryIndexTo(int index) {
    Preconditions.checkArgument(
        index >= getHighestAppliedEntryIndex(),
        String.format(
            "Attempting to set applied entry index [%d] lower than current value [%d].",
            index, getHighestAppliedEntryIndex()));
    Preconditions.checkArgument(
        index <= getHighestCommittedEntryIndex(),
        String.format(
            "Attempting to set applied entry index [%d] higher than committed entry index [%d]",
            index, getHighestCommittedEntryIndex()));
    int prev = highestAppliedEntryIndex.getAndSet(index);
    logger.atInfo().log("Set applied entry index to [%d], previous value [%d].", index, prev);
  }

  /** Returns the {@link RaftServerId} of the current leader, if known. */
  Optional<RaftServerId> getLeaderServerId() {
    return Optional.ofNullable(leaderServerId);
  }

  void setLeaderServerId(RaftServerId leaderServerId) {
    Preconditions.checkArgument(
        leaderServerId != null,
        "Leader's server id cannot be set to null. Use clearLeaderServerId().");
    this.leaderServerId = leaderServerId;
  }

  void clearLeaderServerId() {
    this.leaderServerId = null;
  }
}

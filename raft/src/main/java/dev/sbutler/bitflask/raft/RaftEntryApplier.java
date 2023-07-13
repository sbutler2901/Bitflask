package dev.sbutler.bitflask.raft;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.raft.exceptions.RaftException;
import jakarta.inject.Inject;

/** Handles applying committed entries to the {@link RaftLog}. */
final class RaftEntryApplier extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftLog raftLog;
  private final RaftCommandTopic raftCommandTopic;
  private final RaftVolatileState raftVolatileState;
  private final RaftCommandConverter raftCommandConverter;

  private volatile boolean shouldContinueExecuting = true;

  @Inject
  RaftEntryApplier(
      RaftLog raftLog,
      RaftCommandTopic raftCommandTopic,
      RaftVolatileState raftVolatileState,
      RaftCommandConverter raftCommandConverter) {
    this.raftLog = raftLog;
    this.raftCommandTopic = raftCommandTopic;
    this.raftVolatileState = raftVolatileState;
    this.raftCommandConverter = raftCommandConverter;
  }

  @Override
  protected void run() {
    while (shouldContinueExecuting) {
      applyCommittedEntries();
    }
  }

  /** Applies any committed entries that have not been applied yet. */
  private void applyCommittedEntries() {
    int highestAppliedIndex = raftVolatileState.getHighestAppliedEntryIndex();
    int highestCommittedIndex = raftVolatileState.getHighestCommittedEntryIndex();
    if (highestCommittedIndex < highestAppliedIndex) {
      throw new RaftException(
          "The highest applied index is greater than the highest committed index!");
    } else if (highestCommittedIndex == highestAppliedIndex) {
      return;
    }

    for (int nextIndexToApply = highestAppliedIndex + 1;
        nextIndexToApply <= highestCommittedIndex;
        nextIndexToApply++) {
      try {
        raftVolatileState.setHighestAppliedEntryIndex(nextIndexToApply);
        raftCommandTopic.notifyObservers(
            raftCommandConverter.reverse().convert(raftLog.getEntryAtIndex(nextIndexToApply)));
      } catch (Exception e) {
        raftVolatileState.setHighestAppliedEntryIndex(nextIndexToApply - 1);
        logger.atSevere().withCause(e).log(
            "Failed to apply command at index [%d].", nextIndexToApply);
        // TODO: terminate server if unrecoverable?
        break;
      }
    }
  }

  @Override
  protected void triggerShutdown() {
    shouldContinueExecuting = false;
  }
}

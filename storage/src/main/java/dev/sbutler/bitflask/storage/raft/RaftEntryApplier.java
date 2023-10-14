package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.storage.commands.*;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import jakarta.inject.Inject;

/** Handles applying committed entries to the {@link RaftLog}. */
final class RaftEntryApplier extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftLog raftLog;
  private final RaftVolatileState raftVolatileState;
  private final RaftEntryConverter raftEntryConverter;
  private final StorageCommandExecutor storageCommandExecutor;
  private final RaftSubmissionManager raftSubmissionManager;

  private volatile boolean shouldContinueExecuting = true;

  @Inject
  RaftEntryApplier(
      RaftLog raftLog,
      RaftVolatileState raftVolatileState,
      RaftEntryConverter raftEntryConverter,
      StorageCommandExecutor storageCommandExecutor,
      RaftSubmissionManager raftSubmissionManager) {
    this.raftLog = raftLog;
    this.raftVolatileState = raftVolatileState;
    this.raftEntryConverter = raftEntryConverter;
    this.storageCommandExecutor = storageCommandExecutor;
    this.raftSubmissionManager = raftSubmissionManager;
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
        raftVolatileState.increaseHighestAppliedEntryIndexTo(nextIndexToApply);
        Entry entry = raftLog.getEntryAtIndex(nextIndexToApply);
        StorageCommandDto dto = raftEntryConverter.reverse().convert(entry);
        StorageCommandResults results = storageCommandExecutor.executeDto(dto);
        raftSubmissionManager.completeSubmission(nextIndexToApply, results);
      } catch (Exception e) {
        raftVolatileState.increaseHighestAppliedEntryIndexTo(nextIndexToApply - 1);
        logger.atSevere().withCause(e).log(
            "Failed to apply command at index [%d].", nextIndexToApply);
        // TODO: terminate server
        break;
      }
    }
  }

  @Override
  protected void triggerShutdown() {
    shouldContinueExecuting = false;
  }
}

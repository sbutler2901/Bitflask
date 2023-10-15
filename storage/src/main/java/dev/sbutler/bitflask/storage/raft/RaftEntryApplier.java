package dev.sbutler.bitflask.storage.raft;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.storage.commands.*;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import jakarta.inject.Inject;

/** Handles applying committed {@link Entry}s in the {@link RaftLog} to the storage engine. */
final class RaftEntryApplier extends AbstractExecutionThreadService {

  private final RaftLog raftLog;
  private final RaftVolatileState raftVolatileState;
  private final RaftEntryConverter raftEntryConverter;
  private final StorageCommandExecutor storageCommandExecutor;
  private final RaftModeManager raftModeManager;
  private final RaftSubmissionManager raftSubmissionManager;

  private volatile boolean shouldContinueExecuting = true;

  @Inject
  RaftEntryApplier(
      RaftLog raftLog,
      RaftVolatileState raftVolatileState,
      RaftEntryConverter raftEntryConverter,
      StorageCommandExecutor storageCommandExecutor,
      RaftModeManager raftModeManager,
      RaftSubmissionManager raftSubmissionManager) {
    this.raftLog = raftLog;
    this.raftVolatileState = raftVolatileState;
    this.raftEntryConverter = raftEntryConverter;
    this.storageCommandExecutor = storageCommandExecutor;
    this.raftModeManager = raftModeManager;
    this.raftSubmissionManager = raftSubmissionManager;
  }

  @Override
  protected void run() {
    while (shouldContinueExecuting) {
      applyCommittedEntries();
    }
  }

  /** Applies any committed entries that have not been applied yet. */
  @VisibleForTesting
  void applyCommittedEntries() {
    int highestAppliedIndex = raftVolatileState.getHighestAppliedEntryIndex();
    int highestCommittedIndex = raftVolatileState.getHighestCommittedEntryIndex();
    if (highestCommittedIndex == highestAppliedIndex) {
      return;
    }

    for (int nextIndexToApply = highestAppliedIndex + 1;
        nextIndexToApply <= highestCommittedIndex;
        nextIndexToApply++) {
      applyEntryAtIndex(nextIndexToApply);
    }
  }

  /**
   * Applies the {@link Entry} in the {@link RaftLog} at the provided {@code entryIndex} to the
   * storage engine.
   */
  private void applyEntryAtIndex(int entryIndex) {
    Entry entry = raftLog.getEntryAtIndex(entryIndex);
    StorageCommandDto dto = raftEntryConverter.reverse().convert(entry);

    StorageCommandResults results;
    try {
      results = storageCommandExecutor.executeDto(dto);
      raftVolatileState.increaseHighestAppliedEntryIndexTo(entryIndex);
    } catch (Exception e) {
      triggerShutdown();
      RaftException exception =
          new RaftException(
              String.format(
                  "Failed to apply [%s] at index [%d].", entry.getCommandCase(), entryIndex),
              e);
      raftSubmissionManager.completeAllSubmissionsWithFailure(exception);
      throw exception;
    }

    if (raftModeManager.isCurrentLeader()) {
      raftSubmissionManager.completeSubmission(entryIndex, results);
    }
  }

  @Override
  protected void triggerShutdown() {
    shouldContinueExecuting = false;
  }
}

package dev.sbutler.bitflask.storage.raft;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.storage.commands.*;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import jakarta.inject.Inject;

/** Handles applying committed entries to the {@link RaftLog}. */
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
  private void applyCommittedEntries() {
    int highestAppliedIndex = raftVolatileState.getHighestAppliedEntryIndex();
    int highestCommittedIndex = raftVolatileState.getHighestCommittedEntryIndex();
    if (highestCommittedIndex == highestAppliedIndex) {
      return;
    }

    for (int nextIndexToApply = highestAppliedIndex + 1;
        nextIndexToApply <= highestCommittedIndex;
        nextIndexToApply++) {
      try {
        Entry entry = raftLog.getEntryAtIndex(nextIndexToApply);
        StorageCommandDto dto = raftEntryConverter.reverse().convert(entry);
        StorageCommandResults results = storageCommandExecutor.executeDto(dto);
        if (raftModeManager.isCurrentLeader()) {
          raftSubmissionManager.completeSubmission(nextIndexToApply, results);
        }
        raftVolatileState.increaseHighestAppliedEntryIndexTo(nextIndexToApply);
      } catch (Exception e) {
        triggerShutdown();
        throw new RaftException(
            String.format("Failed to apply command at index [%d].", nextIndexToApply), e);
      }
    }
  }

  @Override
  protected void triggerShutdown() {
    shouldContinueExecuting = false;
  }
}

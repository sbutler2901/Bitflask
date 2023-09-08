package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.commands.StorageCommandFactory;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import jakarta.inject.Inject;

/** Handles applying committed entries to the {@link RaftLog}. */
final class RaftEntryApplier extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftLog raftLog;
  private final RaftVolatileState raftVolatileState;
  private final RaftEntryConverter raftEntryConverter;
  private final StorageCommandFactory storageCommandFactory;

  private volatile boolean shouldContinueExecuting = true;

  @Inject
  RaftEntryApplier(
      RaftLog raftLog,
      RaftVolatileState raftVolatileState,
      RaftEntryConverter raftEntryConverter,
      StorageCommandFactory storageCommandFactory) {
    this.raftLog = raftLog;
    this.raftVolatileState = raftVolatileState;
    this.raftEntryConverter = raftEntryConverter;
    this.storageCommandFactory = storageCommandFactory;
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
        Entry entry = raftLog.getEntryAtIndex(nextIndexToApply);
        StorageCommandDto dto = raftEntryConverter.reverse().convert(entry);
        StorageCommand storageCommand = storageCommandFactory.create(dto);
        storageCommand.execute();
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

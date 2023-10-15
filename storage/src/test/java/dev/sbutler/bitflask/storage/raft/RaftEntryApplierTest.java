package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.storage.commands.StorageCommandExecutor;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link RaftEntryApplier}. */
public class RaftEntryApplierTest {

  private final RaftLog raftLog = new RaftLog();
  private final RaftVolatileState raftVolatileState = new RaftVolatileState();
  private final RaftEntryConverter raftEntryConverter =
      new RaftEntryConverter(mock(RaftPersistentState.class));
  private final StorageCommandExecutor storageCommandExecutor = mock(StorageCommandExecutor.class);
  private final RaftModeManager raftModeManager = mock(RaftModeManager.class);
  private final RaftSubmissionManager raftSubmissionManager = mock(RaftSubmissionManager.class);

  private final RaftEntryApplier raftEntryApplier =
      new RaftEntryApplier(
          raftLog,
          raftVolatileState,
          raftEntryConverter,
          storageCommandExecutor,
          raftModeManager,
          raftSubmissionManager);

  @BeforeEach
  public void beforeEach() {
    raftVolatileState.initialize(0, 0);
  }

  @Test
  public void applyCommittedEntries_appliedAndCommittedIndexAreEqual_earlyReturn() {
    assertThat(raftVolatileState.getHighestAppliedEntryIndex())
        .isEqualTo(raftVolatileState.getHighestCommittedEntryIndex());

    raftEntryApplier.applyCommittedEntries();

    verify(storageCommandExecutor, never()).executeDto(any());
  }

  @Test
  public void applyCommittedEntries_asFollower_success() {
    raftLog.appendEntry(
        Entry.newBuilder()
            .setTerm(0)
            .setSetCommand(SetCommand.newBuilder().setKey("key").setValue("value"))
            .build());
    raftVolatileState.increaseHighestCommittedEntryIndexTo(1);
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(0);

    raftEntryApplier.applyCommittedEntries();

    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(1);
    verify(storageCommandExecutor, times(1)).executeDto(any());
    verify(raftSubmissionManager, never()).completeSubmission(anyInt(), any());
  }

  @Test
  public void applyCommittedEntries_asLeader_success() {
    raftLog.appendEntry(
        Entry.newBuilder()
            .setTerm(0)
            .setSetCommand(SetCommand.newBuilder().setKey("key").setValue("value"))
            .build());
    raftVolatileState.increaseHighestCommittedEntryIndexTo(1);
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(0);
    when(raftModeManager.isCurrentLeader()).thenReturn(true);

    raftEntryApplier.applyCommittedEntries();

    verify(storageCommandExecutor, times(1)).executeDto(any());
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(1);
    verify(raftSubmissionManager, times(1)).completeSubmission(anyInt(), any());
  }

  @Test
  public void applyCommittedEntries_applyFailed_throwsRaftException() {
    raftLog.appendEntry(
        Entry.newBuilder()
            .setTerm(0)
            .setSetCommand(SetCommand.newBuilder().setKey("key").setValue("value"))
            .build());
    raftVolatileState.increaseHighestCommittedEntryIndexTo(1);
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(0);
    RuntimeException exception = new RuntimeException("test");
    when(storageCommandExecutor.executeDto(any())).thenThrow(exception);

    RaftException thrownException =
        assertThrows(RaftException.class, raftEntryApplier::applyCommittedEntries);

    assertThat(thrownException).hasCauseThat().isEqualTo(exception);
    assertThat(thrownException)
        .hasMessageThat()
        .isEqualTo("Failed to apply [SET_COMMAND] at index [1].");
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(0);
    verify(raftSubmissionManager, times(1)).completeAllSubmissionsWithFailure(thrownException);
  }
}

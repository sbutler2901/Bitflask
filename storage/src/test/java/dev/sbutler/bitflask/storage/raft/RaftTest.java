package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.storage.StorageSubmitResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftException;
import org.junit.jupiter.api.Test;

public class RaftTest {

  private static final StorageCommandDto COMMAND_DTO =
      new StorageCommandDto.WriteDto("key", "value");

  private final RaftModeManager raftModeManager = mock(RaftModeManager.class);

  private final Raft raft = new Raft(raftModeManager);

  @Test
  public void submitCommand_success() {
    raft.submitCommand(COMMAND_DTO);

    verify(raftModeManager, times(1)).submitCommand(COMMAND_DTO);
  }

  @Test
  public void submitCommand_raftExceptionThrown_successResultsWithFailedFuture() {
    RaftException exception = new RaftException("test");
    when(raftModeManager.submitCommand(COMMAND_DTO)).thenThrow(exception);

    StorageSubmitResults submitResults = raft.submitCommand(COMMAND_DTO);

    assertThat(submitResults).isInstanceOf(StorageSubmitResults.Success.class);
    var submitFuture = ((StorageSubmitResults.Success) submitResults).submitFuture();
    assertThat(submitFuture.exceptionNow()).isEqualTo(exception);
  }

  @Test
  public void submitCommand_generalExceptionThrown_successResultsWithFailedFuture() {
    RuntimeException exception = new RuntimeException("test");
    when(raftModeManager.submitCommand(COMMAND_DTO)).thenThrow(exception);

    StorageSubmitResults submitResults = raft.submitCommand(COMMAND_DTO);

    assertThat(submitResults).isInstanceOf(StorageSubmitResults.Success.class);
    var submitFuture = ((StorageSubmitResults.Success) submitResults).submitFuture();
    assertThat(submitFuture.exceptionNow()).isInstanceOf(RaftException.class);
    assertThat(submitFuture.exceptionNow())
        .hasMessageThat()
        .isEqualTo("Unknown error while submitting.");
    assertThat(submitFuture.exceptionNow()).hasCauseThat().isEqualTo(exception);
  }
}

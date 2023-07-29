package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.StorageResponse.Success;
import dev.sbutler.bitflask.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DeleteCommand}. */
public class DeleteCommandTest {

  private DeleteCommand command;

  private final StorageService storageService = mock(StorageService.class);
  private final String key = "key";

  @BeforeEach
  void beforeEach() {
    command = new DeleteCommand(storageService, key);
  }

  @Test
  void execute() {
    StorageResponse.Success storageResponse = new Success("OK");
    doReturn(storageResponse).when(storageService).processCommand(any());

    String result = command.execute();

    assertThat(result).isEqualTo(storageResponse.message());
  }

  @Test
  void execute_deleteFailed() {
    StorageResponse.Failed storageResponse = new Failed("error");
    doReturn(storageResponse).when(storageService).processCommand(any());

    String result = command.execute();

    assertThat(result).isEqualTo(String.format("Failed to delete [%s]", key));
  }

  @Test
  void execute_storageException() {
    doThrow(new RuntimeException("test")).when(storageService).processCommand(any());

    StorageProcessingException exception =
        assertThrows(StorageProcessingException.class, () -> command.execute());

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(String.format("Unexpected failure deleting [%s]", key));
  }
}

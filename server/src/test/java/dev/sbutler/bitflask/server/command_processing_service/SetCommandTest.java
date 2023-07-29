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

/** Unit tests for {@link SetCommand}. */
public class SetCommandTest {

  private SetCommand command;

  private final StorageService storageService = mock(StorageService.class);
  private final String key = "key";
  private final String value = "value";

  @BeforeEach
  void beforeEach() {
    command = new SetCommand(storageService, key, value);
  }

  @Test
  void execute() {
    // Arrange
    StorageResponse.Success storageResponse = new Success("OK");
    doReturn(storageResponse).when(storageService).processCommand(any());

    String result = command.execute();

    assertThat(result).isEqualTo(storageResponse.message());
  }

  @Test
  void execute_writeFailed() {
    // Arrange
    StorageResponse.Failed storageResponse = new Failed("error");
    doReturn(storageResponse).when(storageService).processCommand(any());

    String result = command.execute();

    assertThat(result).isEqualTo(String.format("Failed to write [%s]:[%s]", key, value));
  }

  @Test
  void execute_storageException() {
    doThrow(new RuntimeException("test")).when(storageService).processCommand(any());

    StorageProcessingException exception =
        assertThrows(StorageProcessingException.class, () -> command.execute());

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(String.format("Unexpected failure writing [%s]:[%s]", key, value));
  }
}

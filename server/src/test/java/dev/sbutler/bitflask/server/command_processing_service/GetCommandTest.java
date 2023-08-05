package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.StorageResponse.Success;
import dev.sbutler.bitflask.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GetCommand}. */
public class GetCommandTest {

  private GetCommand command;

  private final StorageService storageService = mock(StorageService.class);
  private final String key = "key";

  @BeforeEach
  void beforeEach() {
    command = new GetCommand(storageService, key);
  }

  @Test
  void execute() {
    StorageResponse.Success storageResponse = new Success("value");
    doReturn(storageResponse).when(storageService).processCommand(any());

    String result = command.execute();

    assertThat(result).isEqualTo(storageResponse.message());
  }

  @Test
  void execute_readFailed() {
    StorageResponse.Failed storageResponse = new Failed("error");
    doReturn(storageResponse).when(storageService).processCommand(any());

    String result = command.execute();

    assertThat(result).isEqualTo(String.format("Failed to read [%s]", key));
  }

  @Test
  void execute_storageException() {
    doThrow(new RuntimeException("test")).when(storageService).processCommand(any());

    StorageProcessingException exception =
        assertThrows(StorageProcessingException.class, () -> command.execute());

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(String.format("Unexpected failure getting [%s]", key));
  }
}

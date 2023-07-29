package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.util.concurrent.Futures.immediateFailedFuture;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.StorageResponse.Success;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

public class SetCommandTest {

  SetCommand setCommand;

  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();

  StorageCommandDispatcher storageCommandDispatcher;
  String key = "key";
  String value = "value";

  @BeforeEach
  void beforeEach() {
    storageCommandDispatcher = mock(StorageCommandDispatcher.class);
    setCommand = new SetCommand(executorService, storageCommandDispatcher, key, value);
  }

  @Test
  void execute() throws Exception {
    // Arrange
    StorageResponse storageResponse = new Success("OK");
    doReturn(immediateFuture(storageResponse)).when(storageCommandDispatcher).put(any());
    // Act
    ListenableFuture<String> executeFuture = setCommand.execute();
    // Assert
    assertTrue(executeFuture.isDone());
    assertEquals("OK", executeFuture.get());
  }

  @Test
  void execute_writeFailed() throws Exception {
    // Arrange
    StorageResponse storageResponse = new Failed("error");
    doReturn(immediateFuture(storageResponse)).when(storageCommandDispatcher).put(any());
    // Act
    ListenableFuture<String> executeFuture = setCommand.execute();
    // Assert
    assertTrue(executeFuture.isDone());
    assertTrue(executeFuture.get().toLowerCase().contains("failed"));
  }

  @Test
  void execute_storageException() throws Exception {
    // Arrange
    doReturn(immediateFailedFuture(new RuntimeException("test")))
        .when(storageCommandDispatcher)
        .put(any());
    // Act
    ListenableFuture<String> executeFuture = setCommand.execute();
    // Assert
    assertTrue(executeFuture.isDone());
    assertTrue(executeFuture.get().toLowerCase().contains("unexpected"));
  }
}

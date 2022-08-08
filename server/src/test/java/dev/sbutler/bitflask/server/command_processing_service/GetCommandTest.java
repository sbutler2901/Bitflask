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
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;

public class GetCommandTest {

  GetCommand getCommand;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  StorageCommandDispatcher storageCommandDispatcher;
  String key = "key";

  @BeforeEach
  void beforeEach() {
    storageCommandDispatcher = mock(StorageCommandDispatcher.class);
    getCommand = new GetCommand(executorService, storageCommandDispatcher, key);
  }

  @Test
  void execute() throws Exception {
    // Arrange
    StorageResponse storageResponse = new Success("value");
    doReturn(immediateFuture(storageResponse)).when(storageCommandDispatcher).put(any());
    // Act
    ListenableFuture<String> executeFuture = getCommand.execute();
    // Assert
    assertTrue(executeFuture.isDone());
    assertEquals("value", executeFuture.get());
  }

  @Test
  void execute_readFailed() throws Exception {
    // Arrange
    StorageResponse storageResponse = new Failed("error");
    doReturn(immediateFuture(storageResponse)).when(storageCommandDispatcher).put(any());
    // Act
    ListenableFuture<String> executeFuture = getCommand.execute();
    // Assert
    assertTrue(executeFuture.isDone());
    assertTrue(executeFuture.get().toLowerCase().contains("failed"));
  }

  @Test
  void execute_storageException() throws Exception {
    // Arrange
    doReturn(immediateFailedFuture(new RuntimeException("test")))
        .when(storageCommandDispatcher).put(any());
    // Act
    ListenableFuture<String> executeFuture = getCommand.execute();
    // Assert
    assertTrue(executeFuture.isDone());
    assertTrue(executeFuture.get().toLowerCase().contains("unexpected"));
  }
}

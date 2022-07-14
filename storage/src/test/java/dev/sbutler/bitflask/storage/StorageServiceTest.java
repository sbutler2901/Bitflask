package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommand.Type;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Status;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

  @InjectMocks
  StorageService storage;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  SegmentManager segmentManager;
  @Mock
  StorageCommandDispatcher storageCommandDispatcher;

  @Test
  void read() throws Exception {
    // Arrange
    String key = "key", value = "value";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommand, StorageResponse> submission =
        new DispatcherSubmission<>(
            new StorageCommand(Type.READ, ImmutableList.of(key)),
            responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doReturn(Optional.of(value)).when(segmentManager).read(anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(1);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertEquals(Status.OK, response.status());
    assertEquals(value, response.response().get());
  }

  @Test
  void read_IOException() throws Exception {
    // Arrange
    String key = "key";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommand, StorageResponse> submission =
        new DispatcherSubmission<>(
            new StorageCommand(Type.READ, ImmutableList.of(key)),
            responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doThrow(IOException.class).when(segmentManager).read(anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(1);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertEquals(Status.FAILED, response.status());
    assertTrue(response.errorMessage().isPresent());
  }

  @Test
  void write() throws Exception {
    // Arrange
    String key = "key", value = "value";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommand, StorageResponse> submission =
        new DispatcherSubmission<>(
            new StorageCommand(Type.WRITE, ImmutableList.of(key, value)),
            responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(1);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertEquals(Status.OK, response.status());
  }

  @Test
  void write_IOException() throws Exception {
    // Arrange
    String key = "key", value = "value";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommand, StorageResponse> submission =
        new DispatcherSubmission<>(
            new StorageCommand(Type.WRITE, ImmutableList.of(key, value)),
            responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doThrow(IOException.class).when(segmentManager).write(anyString(), anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(1);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertEquals(Status.FAILED, response.status());
    assertTrue(response.errorMessage().isPresent());
  }

  @Test
  void shutdown() throws Exception {
    // Arrange
    storage.startAsync().awaitRunning();
    assertTrue(storage.isRunning());
    // Act
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertFalse(storage.isRunning());
  }
}

package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
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
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(new ReadDTO(key), responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doReturn(Optional.of(value)).when(segmentManager).read(anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(100);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertInstanceOf(StorageResponse.Success.class, response);
    StorageResponse.Success success = (Success) response;
    assertEquals(value, success.message());
  }

  @Test
  void read_keyNotFound() throws Exception {
    // Arrange
    String key = "key";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(new ReadDTO(key), responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doReturn(Optional.empty()).when(segmentManager).read(anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(100);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertInstanceOf(StorageResponse.Success.class, response);
    StorageResponse.Success success = (Success) response;
    assertTrue(success.message().toLowerCase().contains("not found"));
  }

  @Test
  void read_IOException() throws Exception {
    // Arrange
    String key = "key";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(new ReadDTO(key), responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doThrow(IOException.class).when(segmentManager).read(anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(100);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertInstanceOf(StorageResponse.Failed.class, response);
  }

  @Test
  void write() throws Exception {
    // Arrange
    String key = "key", value = "value";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(new WriteDTO(key, value), responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(100);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertInstanceOf(StorageResponse.Success.class, response);
  }

  @Test
  void write_IOException() throws Exception {
    // Arrange
    String key = "key", value = "value";
    SettableFuture<StorageResponse> responseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(new WriteDTO(key, value), responseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    doThrow(IOException.class).when(segmentManager).write(anyString(), anyString());
    // Act
    storage.startAsync().awaitRunning();
    Thread.sleep(100);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
    // Assert
    assertTrue(responseFuture.isDone());
    StorageResponse response = responseFuture.get();
    assertInstanceOf(StorageResponse.Failed.class, response);
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

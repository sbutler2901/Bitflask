package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.time.Duration;
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
  SegmentManagerService segmentManagerService;
  @Mock
  StorageCommandDispatcher storageCommandDispatcher;
  @Mock
  CommandMapper commandMapper;

  @SuppressWarnings("unchecked")
  @Test
  void processSubmission() throws Exception {
    // Arrange
    String key = "key", value = "value";
    ReadDTO dto = new ReadDTO(key);
    SettableFuture<StorageResponse> submissionResponseFuture = mock(SettableFuture.class);
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(dto, submissionResponseFuture);
    doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
    StorageCommand command = mock(StorageCommand.class);
    ListenableFuture<StorageResponse> commandResponseFuture = mock(ListenableFuture.class);
    doReturn(commandResponseFuture).when(command).execute();
    doReturn(command).when(commandMapper).mapToCommand(any(StorageCommandDTO.class));
    // Act
    activateStorageService();
    // Assert
    verify(commandMapper, atLeastOnce()).mapToCommand(dto);
    verify(command, atLeastOnce()).execute();
    verify(submissionResponseFuture, atLeastOnce()).setFuture(commandResponseFuture);
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

  private void activateStorageService() throws Exception {
    storage.startAsync().awaitRunning();
    Thread.sleep(100);
    storage.triggerShutdown();
    storage.awaitTerminated(Duration.ofSeconds(1));
  }
}

package dev.sbutler.bitflask.storage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

  StorageService storageService;
  @Mock
  SegmentManagerService segmentManagerService;
  @Mock
  StorageCommandDispatcher storageCommandDispatcher;
  @Mock
  CommandMapper commandMapper;

  @Test
  void doStart() {
    // Arrange
    storageService = new StorageService(segmentManagerService, storageCommandDispatcher,
        commandMapper);
    // Act
    storageService.startUp();
    // Assert
    verify(segmentManagerService).startAsync();
    verify(segmentManagerService).awaitRunning();
  }

  @SuppressWarnings("unchecked")
  @Test
  void run() throws Exception {
    // Arrange
    storageService = new StorageService(segmentManagerService,
        storageCommandDispatcher, commandMapper);
    ReadDTO dto = new ReadDTO("key");
    SettableFuture<StorageResponse> submissionResponseFuture = mock(SettableFuture.class);
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(dto, submissionResponseFuture);
    doReturn(Optional.of(submission)).when(storageCommandDispatcher)
        .poll(anyLong(), any(TimeUnit.class));

    StorageCommand command = mock(StorageCommand.class);
    ListenableFuture<StorageResponse> commandResponseFuture = mock(ListenableFuture.class);
    doReturn(commandResponseFuture).when(command).execute();
    doReturn(command).when(commandMapper).mapToCommand(any(StorageCommandDTO.class));

    when(segmentManagerService.isRunning()).thenReturn(true).thenReturn(false);
    // Act
    storageService.run();
    // Assert
    verify(commandMapper, atLeastOnce()).mapToCommand(dto);
    verify(command, atLeastOnce()).execute();
    verify(submissionResponseFuture, atLeastOnce()).setFuture(commandResponseFuture);
  }

  @Test
  void triggerShutdown() {
    // Arrange
    storageService = new StorageService(segmentManagerService,
        storageCommandDispatcher, commandMapper);
    // Act
    storageService.triggerShutdown();
    // Assert
    verify(storageCommandDispatcher).closeAndDrain();
    verify(segmentManagerService).stopAsync();
    verify(segmentManagerService).awaitTerminated();
  }
}

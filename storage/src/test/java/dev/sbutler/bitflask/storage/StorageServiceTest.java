package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.lsm.LSMTreeLoader;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnstableApiUsage")
class StorageServiceTest {

  private final StorageCommandDispatcher storageCommandDispatcher =
      mock(StorageCommandDispatcher.class);
  private final CommandMapper commandMapper = mock(CommandMapper.class);
  private final LSMTreeLoader lsmTreeLoader = mock(LSMTreeLoader.class);

  private final StorageService storageService = new StorageService(
      TestingExecutors.sameThreadScheduledExecutor(),
      storageCommandDispatcher,
      commandMapper,
      lsmTreeLoader);

  @Test
  void run() throws Exception {
    // Arrange
    ReadDTO dto = new ReadDTO("key");
    SettableFuture<StorageResponse> submissionResponseFuture = SettableFuture.create();
    DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
        new DispatcherSubmission<>(dto, submissionResponseFuture);
    when(storageCommandDispatcher.poll(anyLong(), any(TimeUnit.class)))
        .thenReturn(Optional.of(submission));

    StorageCommand command = mock(StorageCommand.class);
    StorageResponse response = new Success("value");
    when(command.execute()).thenReturn(new Success("value"));
    when(commandMapper.mapToCommand(any())).thenReturn(command);

    // Act
    Thread serviceThread = Thread.ofVirtual().start(storageService::run);
    Thread.sleep(100);
    serviceThread.interrupt();
    serviceThread.join();

    // Assert
    assertThat(submissionResponseFuture.isDone()).isTrue();
    assertThat(submissionResponseFuture.get()).isEqualTo(response);
  }

  @Test
  void triggerShutdown() {
    storageService.triggerShutdown();

    verify(storageCommandDispatcher).closeAndDrain();
  }
}

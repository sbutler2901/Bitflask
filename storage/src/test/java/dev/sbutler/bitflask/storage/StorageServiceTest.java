package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@SuppressWarnings("UnstableApiUsage")
class StorageServiceTest {

  private final StorageCommandDispatcher storageCommandDispatcher =
      mock(StorageCommandDispatcher.class);
  private final CommandMapper commandMapper = mock(CommandMapper.class);
  private final StorageLoader storageLoader = mock(StorageLoader.class);

  private final StorageService storageService = new StorageService(
      TestingExecutors.sameThreadScheduledExecutor(),
      storageCommandDispatcher,
      commandMapper,
      storageLoader);

  @Test
  public void startUp() {
    storageService.startUp();

    verify(storageLoader, times(1)).load();
  }

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
    assertTimeoutPreemptively(Duration.ofSeconds(5), () -> serviceThread.join());

    // Assert
    assertThat(storageService.isRunning()).isFalse();
    verify(storageCommandDispatcher, times(1)).closeAndDrain();

    assertThat(submissionResponseFuture.isDone()).isTrue();
    assertThat(submissionResponseFuture.get()).isEqualTo(response);
  }

  @Test
  public void run_interruptedWhilePolling() throws Exception {
    InterruptedException interruptedException = new InterruptedException("test");
    when(storageCommandDispatcher.poll(anyLong(), any(TimeUnit.class)))
        .thenThrow(interruptedException);

    Thread serviceThread = Thread.ofVirtual().start(storageService::run);
    Thread.sleep(100);
    assertTimeoutPreemptively(Duration.ofSeconds(5), () -> serviceThread.join());

    assertThat(storageService.isRunning()).isFalse();
    verify(storageCommandDispatcher, times(1)).closeAndDrain();
  }

  @Test
  void triggerShutdown() {
    storageService.triggerShutdown();

    verify(storageCommandDispatcher).closeAndDrain();
  }
}

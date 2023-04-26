package dev.sbutler.bitflask.storage.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import java.time.Duration;

/**
 * Helper for interacting with a
 * {@link dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher}.
 */
@SuppressWarnings("UnstableApiUsage")
public class StorageCommandDispatcherHelper {

  private final StorageCommandDispatcher commandDispatcher;
  private final ListeningExecutorService listeningExecutorService;

  public StorageCommandDispatcherHelper(StorageCommandDispatcher commandDispatcher,
      ListeningExecutorService listeningExecutorService) {
    this.commandDispatcher = commandDispatcher;
    this.listeningExecutorService = listeningExecutorService;
  }

  public ListenableFuture<StorageResponse> submitStorageCommand(StorageCommandDTO command) {
    return commandDispatcher.put(command);
  }

  /**
   * Submits the provided {@link dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO}s, only
   * blocked by {@link dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher#put}.
   */
  public ImmutableList<ListenableFuture<StorageResponse>> submitStorageCommandsSequentially(
      ImmutableList<StorageCommandDTO> commands) {
    return commands.stream().map(commandDispatcher::put).collect(toImmutableList());
  }

  /**
   * Submits the provided {@link dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO}s
   * blocking for {@code delay} each time {@code delayAfterNumSubmitted} is reached.
   */
  public ImmutableList<ListenableFuture<StorageResponse>> submitStorageCommandsSequentiallyWithDelay(
      ImmutableList<StorageCommandDTO> commands, Duration delay, int delayAfterNumSubmitted) {
    ImmutableList.Builder<ListenableFuture<StorageResponse>> responses = ImmutableList.builder();
    for (int i = 0; i < commands.size(); i++) {
      responses.add(commandDispatcher.put(commands.get(i)));
      if (i % delayAfterNumSubmitted == 0) {
        try {
          Thread.sleep(delay);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return responses.build();
  }

  public ListenableFuture<ImmutableList<ListenableFuture<StorageResponse>>> combineResponseFutures(
      ImmutableList<ListenableFuture<StorageResponse>> responseFutures) {
    return Futures.whenAllComplete(responseFutures)
        .call(() -> responseFutures.stream().collect(toImmutableList()), listeningExecutorService);
  }

  public StorageResponse.Success getResponseAsSuccess(
      ListenableFuture<StorageResponse> responseFuture) throws Exception {
    StorageResponse response = responseFuture.get();

    assertThat(response).isInstanceOf(StorageResponse.Success.class);
    return (StorageResponse.Success) response;
  }

  public StorageResponse.Failed getResponseAsFailed(
      ListenableFuture<StorageResponse> responseFuture) throws Exception {
    StorageResponse response = responseFuture.get();

    assertThat(response).isInstanceOf(StorageResponse.Failed.class);
    return (StorageResponse.Failed) response;
  }
}

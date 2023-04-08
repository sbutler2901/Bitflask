package dev.sbutler.bitflask.storage.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;

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
      ImmutableList<StorageCommandDTO> commands, int delayAfterNumSubmitted, Duration delay) {
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

  public ListenableFuture<ImmutableList<Result>> combineResponseFutures(
      ImmutableList<ListenableFuture<StorageResponse>> responseFutures) {
    return Futures.whenAllComplete(responseFutures)
        .call(() -> responseFutures.stream()
                .map(this::mapDoneResponseFutureToResult)
                .collect(toImmutableList()),
            listeningExecutorService);
  }

  private Result mapDoneResponseFutureToResult(ListenableFuture<StorageResponse> responseFuture) {
    return switch (responseFuture.state()) {
      case SUCCESS -> new Result.Success(responseFuture.resultNow());
      case FAILED -> new Result.Failed(responseFuture.exceptionNow());
      default -> throw new RuntimeException("Storage was not SUCCESS or FAILED");
    };
  }

  /**
   * The result of a test storage submission future.
   */
  sealed interface Result {

    record Success(StorageResponse storageResponse) implements Result {

    }

    record Failed(Throwable failure) implements Result {

    }
  }
}

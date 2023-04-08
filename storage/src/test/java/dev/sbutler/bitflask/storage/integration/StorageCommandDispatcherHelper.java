package dev.sbutler.bitflask.storage.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import java.time.Duration;

/**
 * Helper for interacting with a
 * {@link dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher}.
 */
public class StorageCommandDispatcherHelper {

  private final StorageCommandDispatcher commandDispatcher;

  public StorageCommandDispatcherHelper(StorageCommandDispatcher commandDispatcher) {
    this.commandDispatcher = commandDispatcher;
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
}

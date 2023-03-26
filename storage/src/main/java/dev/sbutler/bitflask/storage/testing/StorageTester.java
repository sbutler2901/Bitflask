package dev.sbutler.bitflask.storage.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.testing.StorageTester.Result.Failed;
import dev.sbutler.bitflask.storage.testing.StorageTester.Result.Success;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings("UnstableApiUsage")
@Singleton
final class StorageTester implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final StorageConfigurations configurations;
  private final StorageCommandDispatcher storageCommandDispatcher;

  @Inject
  StorageTester(ListeningExecutorService listeningExecutorService,
      StorageConfigurations configurations,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.listeningExecutorService = listeningExecutorService;
    this.configurations = configurations;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  @Override
  public void run() {
    logger.atInfo().log("Starting testing");

    ImmutableList<StorageCommandDTO> storageCommands = getStorageCommands();
    ImmutableList<ListenableFuture<StorageResponse>> responseFutures =
        submitStorageCommandsSequentially(storageCommands);
    ListenableFuture<ImmutableList<Result>> resultsFuture =
        combineResponseFutures(responseFutures);

    ImmutableList<Result> results;
    try {
      Thread.sleep(Duration.ofMillis(1000 + configurations.getCompactorExecDelayMilliseconds()));
      results = resultsFuture.get();
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("Failed to get results");
      return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.atSevere().withCause(e).log("Interrupted while getting results");
      return;
    }

    ImmutableMap<StorageCommandDTO, Result> mappedResults =
        mapCommandsToResults(storageCommands, results);

    logResults(mappedResults);

    logger.atInfo().log("Completed testing");
  }

  private ImmutableList<StorageCommandDTO> getStorageCommands() {
    ImmutableList.Builder<StorageCommandDTO> commands = ImmutableList.builder();
    for (int i = 0; i < 50; i++) {
      commands.add(new WriteDTO("key_" + i, "value_" + i));
    }
    for (int i = 0; i < 20; i++) {
      commands.add(new DeleteDTO("key_" + i));
    }
    for (int i = 0; i < 50; i++) {
      commands.add(new ReadDTO("key_" + i));
    }
    return commands.build();
  }

  private ImmutableList<ListenableFuture<StorageResponse>> submitStorageCommandsSequentially(
      ImmutableList<StorageCommandDTO> commands) {
    return commands.stream().map(storageCommandDispatcher::put).collect(toImmutableList());
  }

  private ListenableFuture<ImmutableList<Result>> combineResponseFutures(
      ImmutableList<ListenableFuture<StorageResponse>> responseFutures) {
    return Futures.whenAllComplete(responseFutures)
        .call(() -> responseFutures.stream()
                .map(this::mapDoneResponseFutureToResult)
                .collect(toImmutableList()),
            listeningExecutorService);
  }

  private Result mapDoneResponseFutureToResult(ListenableFuture<StorageResponse> responseFuture) {
    try {
      return new Result.Success(Futures.getDone(responseFuture));
    } catch (Exception e) {
      return new Result.Failed(e.getCause());
    }
  }

  private ImmutableMap<StorageCommandDTO, Result> mapCommandsToResults(
      ImmutableList<StorageCommandDTO> commands,
      ImmutableList<Result> results) {
    if (commands.size() != results.size()) {
      throw new RuntimeException(String.format("Command size [%d] did not match results size [%d]",
          commands.size(), results.size()));
    }

    ImmutableMap.Builder<StorageCommandDTO, Result> mappedResults = ImmutableMap.builder();

    for (int i = 0; i < commands.size(); i++) {
      mappedResults.put(commands.get(i), results.get(i));
    }

    return mappedResults.buildOrThrow();
  }

  private void logResults(ImmutableMap<StorageCommandDTO, Result> mappedResults) {
    for (var entry : mappedResults.entrySet()) {
      switch (entry.getValue()) {
        case Success success ->
            logger.atInfo().log("[%s]:[%s]", entry.getKey(), success.storageResponse());
        case Failed failed ->
            logger.atWarning().log("[%s]:[%s]", entry.getKey(), failed.failure().getMessage());
      }
    }
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

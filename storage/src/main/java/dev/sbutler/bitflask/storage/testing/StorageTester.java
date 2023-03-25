package dev.sbutler.bitflask.storage.testing;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;
import javax.inject.Singleton;

@SuppressWarnings("UnstableApiUsage")
@Singleton
final class StorageTester implements Runnable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final StorageCommandDispatcher storageCommandDispatcher;

  @Inject
  StorageTester(ListeningExecutorService listeningExecutorService,
      StorageCommandDispatcher storageCommandDispatcher) {
    this.listeningExecutorService = listeningExecutorService;
    this.storageCommandDispatcher = storageCommandDispatcher;
  }

  @Override
  public void run() {
    logger.atInfo().log("Starting testing");

    StorageCommandDTO writeDto = new StorageCommandDTO.WriteDTO("key", "value");
    StorageCommandDTO readDto = new StorageCommandDTO.ReadDTO("key");

    ListenableFuture<List<StorageResponse>> results = Futures.successfulAsList(ImmutableList.of(
        storageCommandDispatcher.put(writeDto),
        storageCommandDispatcher.put(readDto)));

    try {
      results.get();
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("Failed to get results");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.atSevere().withCause(e).log("Interrupted while getting results");
    }

    logger.atInfo().log("Completed testing");
  }
}

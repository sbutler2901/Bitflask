package dev.sbutler.bitflask.storage.testing;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import javax.inject.Inject;
import javax.inject.Singleton;

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
    logger.atInfo().log("Testing");
  }
}

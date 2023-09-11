package dev.sbutler.bitflask.storage.commands;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import jakarta.inject.Inject;

/** Handles executing {@link StorageCommand}s. */
public final class StorageCommandExecutor {

  private final ListeningExecutorService executorService;
  private final StorageCommandFactory storageCommandFactory;

  @Inject
  StorageCommandExecutor(
      ListeningExecutorService executorService, StorageCommandFactory storageCommandFactory) {
    this.executorService = executorService;
    this.storageCommandFactory = storageCommandFactory;
  }

  /**
   * Converts the {@link dev.sbutler.bitflask.storage.commands.StorageCommandDto} into a {@link
   * dev.sbutler.bitflask.storage.commands.StorageCommand} and submits it for asynchronous
   * execution.
   */
  public ListenableFuture<StorageCommandResults> submitDto(StorageCommandDto storageCommandDto) {
    StorageCommand storageCommand = storageCommandFactory.create(storageCommandDto);
    return Futures.submit(storageCommand::execute, executorService);
  }

  /**
   * Converts the {@link dev.sbutler.bitflask.storage.commands.StorageCommandDto} into a {@link
   * dev.sbutler.bitflask.storage.commands.StorageCommand} and synchronously executes it.
   */
  public StorageCommandResults executeDto(StorageCommandDto storageCommandDto) {
    StorageCommand storageCommand = storageCommandFactory.create(storageCommandDto);
    return storageCommand.execute();
  }
}

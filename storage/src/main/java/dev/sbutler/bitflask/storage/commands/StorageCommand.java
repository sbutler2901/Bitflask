package dev.sbutler.bitflask.storage.commands;

import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;

public interface StorageCommand {

  /**
   * Executes the corresponding command
   *
   * @return a ListenableFuture resolving with the results of the executed command
   */
  ListenableFuture<StorageResponse> execute();
}

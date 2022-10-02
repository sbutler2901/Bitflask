package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * A general interface representing any command executed by the server.
 */
interface ServerCommand {

  /**
   * Executes the corresponding command
   *
   * @return a ListenableFuture resolving with the results of the executed command
   */
  ListenableFuture<String> execute();
}

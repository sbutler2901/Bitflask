package dev.sbutler.bitflask.server.command_processing_service;


/**
 * A general interface representing any command executed by the server.
 */
interface ServerCommand {

  /**
   * Executes the corresponding command
   *
   * @return a ListenableFuture resolving with the results of the executed command
   */
  String execute();
}

package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Asynchronously process the server side ping command.
 */
class PingCommand implements ServerCommand {

  @Override
  public ListenableFuture<String> execute() {
    return immediateFuture("pong");
  }
}

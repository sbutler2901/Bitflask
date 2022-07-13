package dev.sbutler.bitflask.server.command_processing_service.commands;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.util.concurrent.ListenableFuture;

public class PingCommand implements ServerCommand {

  @Override
  public ListenableFuture<String> execute() {
    return immediateFuture("pong");
  }
}

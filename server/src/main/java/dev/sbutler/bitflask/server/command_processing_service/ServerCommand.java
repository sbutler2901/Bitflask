package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.util.concurrent.ListenableFuture;

public interface ServerCommand {

  ListenableFuture<String> execute();
}

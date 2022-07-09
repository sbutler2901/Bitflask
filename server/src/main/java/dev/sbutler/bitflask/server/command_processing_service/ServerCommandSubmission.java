package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.util.concurrent.SettableFuture;

public record ServerCommandSubmission(ServerCommand command,
                                      SettableFuture<ServerResponse> response) {

}
package dev.sbutler.bitflask.server.network_service.client_handling_service.processing;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import dev.sbutler.bitflask.server.command_processing_service.ServerCommand;
import dev.sbutler.bitflask.server.command_processing_service.ServerCommandDispatcher;
import dev.sbutler.bitflask.server.command_processing_service.ServerResponse;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class ClientMessageProcessor {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ServerCommandDispatcher serverCommandDispatcher;
  private final RespReader respReader;
  private final RespWriter respWriter;

  @Inject
  ClientMessageProcessor(ServerCommandDispatcher serverCommandDispatcher, RespReader respReader,
      RespWriter respWriter) {
    this.serverCommandDispatcher = serverCommandDispatcher;
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public boolean processNextMessage() {
    try {
      RespType<?> clientMessage = readClientMessage();
      RespType<?> response = getServerResponseToClient(clientMessage);
      writeResponseMessage(response);
      return true;
    } catch (EOFException e) {
      logger.atWarning().log("Client disconnected.");
    } catch (IOException e) {
      // todo: test more
      logger.atSevere().withCause(e).log("Server shutdown while reading client next message");
    }
    return false;
  }

  private RespType<?> readClientMessage() throws IOException {
    return respReader.readNextRespType();
  }

  private RespType<?> getServerResponseToClient(RespType<?> clientMessage) throws IOException {
    logger.atInfo().log("%s received from client", clientMessage);

    ServerCommand command;
    try {
      // todo: differentiate between invalid format and invalid command and terminate connection accordingly
      command = ServerCommand.valueOf(clientMessage);
    } catch (IllegalArgumentException e) {
      return new RespBulkString("Invalid command: " + e.getMessage());
    }

    ListenableFuture<ServerResponse> responseFuture = serverCommandDispatcher.put(command);
    try {
      ServerResponse serverResponse = responseFuture.get();
      return switch (serverResponse.status()) {
        case OK -> new RespBulkString(serverResponse.response().get());
        case FAILED -> new RespBulkString(serverResponse.errorMessage().get());
      };
    } catch (InterruptedException e) {
      logger.atWarning().withCause(e).log("Interrupted while reading response");
      Thread.currentThread().interrupt();
      return new RespBulkString("InternalServer error");
    } catch (ExecutionException e) {
      logger.atWarning().withCause(e).log("Failed to execute command");
      return new RespBulkString("InternalServer error");
    }
  }

  private void writeResponseMessage(RespType<?> response) throws IOException {
    respWriter.writeRespType(response);
  }

}

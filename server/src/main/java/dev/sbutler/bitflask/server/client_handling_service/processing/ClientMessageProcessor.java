package dev.sbutler.bitflask.server.client_handling_service.processing;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import dev.sbutler.bitflask.server.command_processing_service.ServerCommand;
import dev.sbutler.bitflask.server.command_processing_service.ServerCommandDispatcher;
import java.io.EOFException;
import java.io.IOException;
import javax.inject.Inject;

public class ClientMessageProcessor {

  private static final String CLIENT_DISCONNECTED = "Client disconnected.";
  private static final String CLIENT_READ_FAILURE = "Server shutdown while reading client next message";
  private static final String CLIENT_MESSAGE_LOG = "%s received from client";

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
      logger.atWarning().log(CLIENT_DISCONNECTED);
    } catch (IOException e) {
      // todo: test more
      logger.atSevere().withCause(e).log(CLIENT_READ_FAILURE);
    }
    return false;
  }

  private RespType<?> readClientMessage() throws IOException {
    return respReader.readNextRespType();
  }

  private RespType<?> getServerResponseToClient(RespType<?> clientMessage) throws IOException {
    logger.atInfo().log(CLIENT_MESSAGE_LOG, clientMessage);

    String response;
    try {
      // todo: differentiate between invalid format and invalid command and terminate connection accordingly
      ServerCommand command = ServerCommand.valueOf(clientMessage);
      // todo: utilize futures for response
      serverCommandDispatcher.put(command);
      response = "todo";
    } catch (IllegalArgumentException e) {
      response = "Invalid command: " + e.getMessage();
    }

    return new RespBulkString(response);
  }

  private void writeResponseMessage(RespType<?> response) throws IOException {
    respWriter.writeRespType(response);
  }

}

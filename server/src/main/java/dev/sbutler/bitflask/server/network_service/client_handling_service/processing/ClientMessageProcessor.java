package dev.sbutler.bitflask.server.network_service.client_handling_service.processing;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

public class ClientMessageProcessor {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  CommandProcessingService commandProcessingService;
  private final RespReader respReader;
  private final RespWriter respWriter;

  @Inject
  ClientMessageProcessor(CommandProcessingService commandProcessingService, RespReader respReader,
      RespWriter respWriter) {
    this.commandProcessingService = commandProcessingService;
    this.respReader = respReader;
    this.respWriter = respWriter;
  }

  public boolean processNextMessage() {
    try {
      RespType<?> clientMessage = readClientMessage();
      ListenableFuture<String> responseFuture = commandProcessingService.processMessage(
          parseClientMessage(clientMessage));
      RespType<?> response = getServerResponseToClient(responseFuture);
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

  private RespType<?> getServerResponseToClient(ListenableFuture<String> responseFuture)
      throws IOException {
    try {
      return new RespBulkString(responseFuture.get());
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

  private static ImmutableList<String> parseClientMessage(RespType<?> clientMessage) {
    checkNotNull(clientMessage);
    if (!(clientMessage instanceof RespArray clientMessageRespArray)) {
      throw new IllegalArgumentException("Message must be a RespArray");
    }

    return clientMessageRespArray.getValue().stream().map(arg -> {
      if (!(arg instanceof RespBulkString argBulkString)) {
        throw new IllegalArgumentException(
            "Message RespArray must consist of RespBulkString entries");
      }
      return argBulkString.getValue();
    }).collect(toImmutableList());
  }

}

package dev.sbutler.bitflask.server.network_service.client_handling_service.processing;

import static com.google.common.base.Preconditions.checkNotNull;

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
import java.net.ProtocolException;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

/**
 * Handles receiving a client's incoming messages, parsing them, submitting them for processing, and
 * responding.
 */
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

  /**
   * Reads, processes, and responds to the client's message
   *
   * <p>Errors or issues that occur during processing which are unrecoverable will be handled.
   * These cases will result in false being returned by this function.
   *
   * @return true if processing can continue, false otherwise
   */
  public boolean processNextMessage() {
    RespType<?> rawClientMessage = readClientMessage();
    if (rawClientMessage == null) {
      return false;
    }
    ImmutableList<String> clientMessage = parseClientMessage(rawClientMessage);
    if (clientMessage == null) {
      return false;
    }
    ListenableFuture<String> responseFuture = commandProcessingService.processCommandMessage(
        clientMessage);
    RespType<?> response = getServerResponseToClient(responseFuture);
    try {
      if (response == null) {
        response = new RespBulkString("Internal Error. Terminating");
        writeResponseMessage(response);
        return false;
      }
      writeResponseMessage(response);
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to write response to client");
      return false;
    }
    return true;
  }

  private RespType<?> readClientMessage() {
    RespType<?> readValue = null;
    try {
      readValue = respReader.readNextRespType();
    } catch (EOFException e) {
      logger.atWarning().log("Client disconnected.");
    } catch (ProtocolException e) {
      logger.atWarning().withCause(e).log("Client message format malformed");
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failure reading client's message");
    }
    return readValue;
  }

  private RespType<?> getServerResponseToClient(ListenableFuture<String> responseFuture) {
    RespType<?> response = null;
    try {
      response = new RespBulkString(responseFuture.get());
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("Interrupted while reading response");
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("Failed to execute command");
    }
    return response;
  }

  private void writeResponseMessage(RespType<?> response) throws IOException {
    respWriter.writeRespType(response);
  }

  private static ImmutableList<String> parseClientMessage(RespType<?> rawClientMessage) {
    checkNotNull(rawClientMessage);
    ImmutableList.Builder<String> clientMessage = ImmutableList.builder();
    if (!(rawClientMessage instanceof RespArray clientMessageRespArray)) {
      logger.atWarning().log("The client's raw message must be a RespArray");
      return null;
    }
    for (RespType<?> arg : clientMessageRespArray.getValue()) {
      if (!(arg instanceof RespBulkString argBulkString)) {
        logger.atWarning().log("The arguments of the client's raw message must be RespBulkStrings");
        return null;
      }
      clientMessage.add(argBulkString.getValue());
    }
    return clientMessage.build();
  }
}

package dev.sbutler.bitflask.server.network_service;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javax.inject.Inject;

/**
 * Handles receiving a client's incoming messages, parsing them, submitting them for processing, and
 * responding.
 */
final class ClientMessageProcessor implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static final class Factory {

    private final CommandProcessingService commandProcessingService;

    @Inject
    Factory(CommandProcessingService commandProcessingService) {
      this.commandProcessingService = commandProcessingService;
    }

    public ClientMessageProcessor create(RespService respService) {
      return new ClientMessageProcessor(commandProcessingService, respService);
    }
  }

  private final CommandProcessingService commandProcessingService;
  private final RespService respService;

  private volatile boolean isClosed = false;

  private ClientMessageProcessor(CommandProcessingService commandProcessingService,
      RespService respService) {
    this.commandProcessingService = commandProcessingService;
    this.respService = respService;
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
    if (isClosed) {
      return false;
    }
    return readClientMessage()
        .flatMap(this::parseClientMessage)
        .map(commandProcessingService::processCommandMessage)
        .flatMap(this::getServerResponseToClient)
        .map(this::processServerResponse)
        .orElse(false);
  }

  private boolean processServerResponse(RespElement serverResponse) {
    try {
      respService.write(serverResponse);
      return true;
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to write response to client");
    }
    return false;
  }

  private Optional<RespElement> getServerResponseToClient(
      ListenableFuture<String> responseFuture) {
    try {
      return Optional.of(new RespBulkString(responseFuture.get()));
    } catch (InterruptedException e) {
      logger.atSevere().withCause(e).log("Interrupted while reading response");
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      logger.atSevere().withCause(e).log("Failed to execute command");
    }
    return Optional.empty();
  }

  private Optional<ImmutableList<String>> parseClientMessage(RespElement rawClientMessage) {
    ImmutableList.Builder<String> clientMessage = ImmutableList.builder();
    if (!(rawClientMessage instanceof RespArray clientMessageRespArray)) {
      logger.atWarning().log("The client's raw message must be a RespArray");
      return Optional.empty();
    }
    for (RespElement arg : clientMessageRespArray.getValue()) {
      if (!(arg instanceof RespBulkString argBulkString)) {
        logger.atWarning().log("The arguments of the client's raw message must be RespBulkStrings");
        return Optional.empty();
      }
      clientMessage.add(argBulkString.getValue());
    }
    return Optional.of(clientMessage.build());
  }

  private Optional<RespElement> readClientMessage() {
    try {
      return Optional.of(respService.read());
    } catch (EOFException e) {
      logger.atInfo().log("Client disconnected.");
    } catch (ProtocolException e) {
      logger.atWarning().withCause(e).log("Client message format malformed");
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failure reading client's message");
    }
    return Optional.empty();
  }

  public boolean isOpen() {
    return !isClosed;
  }

  @Override
  public void close() throws IOException {
    isClosed = true;
    respService.close();
  }
}

package dev.sbutler.bitflask.server.network_service;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
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

  static final class Factory {

    private final CommandProcessingService commandProcessingService;

    @Inject
    Factory(CommandProcessingService commandProcessingService) {
      this.commandProcessingService = commandProcessingService;
    }

    ClientMessageProcessor create(RespService respService) {
      return new ClientMessageProcessor(commandProcessingService, respService);
    }
  }

  private final CommandProcessingService commandProcessingService;
  private final RespService respService;

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
    if (!respService.isOpen()) {
      return false;
    }

    Optional<RespElement> rawClientMessage = readClientMessage();
    if (rawClientMessage.isEmpty()) {
      return false;
    }

    ParsedClientMessage parsedClientMessage = parseClientMessage(rawClientMessage.get());
    return switch (parsedClientMessage) {
      case ParsedClientMessage.Success success -> processClientMessage(success.clientMessage());
      case ParsedClientMessage.Failure failure -> sendErrorToClient(failure.errorMessage());
    };
  }

  private boolean processClientMessage(ImmutableList<String> clientMessage) {
    ListenableFuture<String> processedResponse =
        commandProcessingService.processCommandMessage(clientMessage);
    return getServerResponseToClient(processedResponse)
        .map(this::sendServerResponse)
        .orElse(false);
  }

  private boolean sendErrorToClient(String errorMessage) {
    logger.atInfo().log("Client sent invalid message, responding with error [%s]", errorMessage);
    RespError respError = new RespError(errorMessage);
    sendServerResponse(respError);
    return false;
  }

  private boolean sendServerResponse(RespElement serverResponse) {
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

  private ParsedClientMessage parseClientMessage(RespElement rawClientMessage) {
    ImmutableList.Builder<String> clientMessage = ImmutableList.builder();
    if (!(rawClientMessage instanceof RespArray clientMessageRespArray)) {
      return new ParsedClientMessage.Failure("Message must be provided in a RespArray");
    }
    for (RespElement arg : clientMessageRespArray.getValue()) {
      if (!(arg instanceof RespBulkString argBulkString)) {
        return new ParsedClientMessage.Failure("Message arguments must be RespBulkStrings");
      }
      clientMessage.add(argBulkString.getValue());
    }
    return new ParsedClientMessage.Success(clientMessage.build());
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
    return respService.isOpen();
  }

  @Override
  public void close() throws IOException {
    respService.close();
  }

  /**
   * Indicates the results of parsing a client's message.
   */
  private sealed interface ParsedClientMessage {

    record Success(ImmutableList<String> clientMessage) implements ParsedClientMessage {

    }

    record Failure(String errorMessage) implements ParsedClientMessage {

    }
  }
}

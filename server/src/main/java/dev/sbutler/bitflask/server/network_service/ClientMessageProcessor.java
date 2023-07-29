package dev.sbutler.bitflask.server.network_service;

import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import dev.sbutler.bitflask.server.command_processing_service.InvalidCommandException;
import dev.sbutler.bitflask.server.command_processing_service.StorageProcessingException;
import jakarta.inject.Inject;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Optional;

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

  private ClientMessageProcessor(
      CommandProcessingService commandProcessingService, RespService respService) {
    this.commandProcessingService = commandProcessingService;
    this.respService = respService;
  }

  /**
   * Reads, processes, and responds to the client's message
   *
   * <p>Errors or issues that occur during processing which are unrecoverable will be handled. These
   * cases will result in false being returned by this function.
   *
   * @return true if processing can continue, false otherwise
   */
  public boolean processNextMessage() {
    if (!respService.isOpen()) {
      return false;
    }
    return readClientMessage().map(this::processClientMessage).orElse(false);
  }

  private boolean processClientMessage(RespElement rawClientMessage) {
    try {
      ImmutableList<String> parsedClientMessage = parseClientMessage(rawClientMessage);
      String storageResponse = commandProcessingService.processCommandMessage(parsedClientMessage);
      RespElement serverResponseToClient = getServerResponseToClient(storageResponse);
      return sendServerResponse(serverResponseToClient);
    } catch (InvalidCommandException e) {
      sendErrorToClient(e.getMessage());
      return true;
    } catch (StorageProcessingException e) {
      return sendErrorToClient(e.getMessage());
    } catch (Exception e) {
      String message = String.format("Unexpected error processing [%s]", rawClientMessage);
      logger.atSevere().withCause(e).log(message);
      return sendErrorToClient(message);
    }
  }

  private RespElement getServerResponseToClient(String response) {
    return new RespBulkString(response);
  }

  private boolean sendServerResponse(RespElement serverResponse) {
    try {
      respService.write(serverResponse);
      return true;
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to write response to client");
      return false;
    }
  }

  private boolean sendErrorToClient(String errorMessage) {
    logger.atInfo().log("Client sent invalid message, responding with error [%s]", errorMessage);
    RespError respError = new RespError(errorMessage);
    sendServerResponse(respError);
    return false;
  }

  private ImmutableList<String> parseClientMessage(RespElement rawClientMessage) {
    ImmutableList.Builder<String> clientMessage = ImmutableList.builder();
    if (!(rawClientMessage instanceof RespArray clientMessageRespArray)) {
      throw new InvalidCommandException("Message must be provided in a RespArray");
    }
    for (RespElement arg : clientMessageRespArray.getValue()) {
      if (!(arg instanceof RespBulkString argBulkString)) {
        throw new InvalidCommandException("Message arguments must be RespBulkStrings");
      }
      clientMessage.add(argBulkString.getValue());
    }
    return clientMessage.build();
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
}

package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import dev.sbutler.bitflask.server.command_processing_service.InvalidCommandException;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Optional;

/**
 * Handles receiving a client's incoming messages, parsing them, submitting them for processing, and
 * responding.
 */
public final class ClientMessageProcessor implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final CommandProcessingService commandProcessingService;
  private final RespService respService;

  @Inject
  ClientMessageProcessor(
      CommandProcessingService commandProcessingService, @Assisted RespService respService) {
    this.commandProcessingService = commandProcessingService;
    this.respService = respService;
  }

  public interface Factory {
    ClientMessageProcessor create(RespService respService);
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

    Optional<RespElement> readRespMessage = readClientRespMessage();
    if (readRespMessage.isEmpty()) {
      return false;
    }

    RespRequest request;
    try {
      request = parseClientMessage(readRespMessage.get());
    } catch (InvalidCommandException e) {
      RespResponse respResponse = new RespResponse.Failure(e.getMessage());
      return sendResponse(respResponse.getAsRespArray());
    }

    return processRespRequest(request);
  }

  private Optional<RespElement> readClientRespMessage() {
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

  private RespRequest parseClientMessage(RespElement rawClientMessage) {
    if (rawClientMessage instanceof RespArray respArray) {
      return RespRequest.createFromRespArray(respArray);
    } else {
      throw new InvalidCommandException("Message must be provided in a RespArray");
    }
  }

  private boolean processRespRequest(RespRequest request) {
    try {
      ClientCommandResults commandResults = commandProcessingService.processRespRequest(request);
      RespResponse respResponse = handleCommandResults(commandResults);
      return sendResponse(respResponse.getAsRespArray());
    } catch (Exception e) {
      return sendUnrecoverableErrorToClient(e);
    }
  }

  private RespResponse handleCommandResults(ClientCommandResults commandResults) {
    return switch (commandResults) {
      case ClientCommandResults.Success success -> new RespResponse.Success(success.message());
      case ClientCommandResults.Failure failure -> new RespResponse.Failure(failure.message());
      case ClientCommandResults.NotCurrentLeader notCurrentLeader -> new RespResponse
          .NotCurrentLeader(
          notCurrentLeader.currentLeaderInfo().getHost(),
          notCurrentLeader.currentLeaderInfo().getRespPort());
      case ClientCommandResults.NoKnownLeader ignored -> new RespResponse.NoKnownLeader();
    };
  }

  private boolean sendUnrecoverableErrorToClient(Exception e) {
    logger.atSevere().withCause(e).log("Responding with unrecoverable error to client.");
    RespError response = new RespError(e.getMessage());
    sendResponse(response);
    return false;
  }

  private boolean sendResponse(RespElement response) {
    try {
      respService.write(response);
      return true;
    } catch (IOException e) {
      logger.atSevere().withCause(e).log("Failed to write response to client");
      return false;
    }
  }

  public boolean isOpen() {
    return respService.isOpen();
  }

  @Override
  public void close() throws IOException {
    respService.close();
  }
}

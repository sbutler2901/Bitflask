package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespServiceProvider;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

/** Commands that should be sent to and executed by a Bitflask server. */
public final class RemoteCommand implements ClientCommand {

  private final RespRequest request;
  private final OutputWriter outputWriter;
  private final RespCommandProcessor respCommandProcessor;
  private final RespServiceProvider respServiceProvider;

  RemoteCommand(
      RespRequest respRequest,
      OutputWriter outputWriter,
      RespCommandProcessor respCommandProcessor,
      RespServiceProvider respServiceProvider) {
    this.request = respRequest;
    this.outputWriter = outputWriter;
    this.respCommandProcessor = respCommandProcessor;
    this.respServiceProvider = respServiceProvider;
  }

  public RespRequest getRespRequest() {
    return request;
  }

  @Override
  public boolean execute() {
    RespResponse response;
    try {
      response = respCommandProcessor.sendRequest(request);
    } catch (ProcessingException e) {
      outputWriter.writeWithNewLine(
          String.format(
              "Failed to process [%s] request. %s", request.getRequestCode(), e.getMessage()));
      return false;
    }
    return handleRespResponse(response);
  }

  private boolean handleRespResponse(RespResponse response) {
    return switch (response) {
      case RespResponse.Success ignored -> {
        outputWriter.writeWithNewLine(response.getMessage());
        yield true;
      }
      case RespResponse.Failure ignored -> {
        outputWriter.writeWithNewLine(response.getMessage());
        yield true;
      }
      case RespResponse.NotCurrentLeader notCurrentLeader -> updateRespServiceForNewLeader(
          notCurrentLeader.getHost(), notCurrentLeader.getRespPort());
      case RespResponse.NoKnownLeader ignored -> {
        outputWriter.writeWithNewLine(response.getMessage());
        yield false;
      }
    };
  }

  private boolean updateRespServiceForNewLeader(String host, int port) {
    RespService respService;
    try {
      respServiceProvider.get().close();
      SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(host, port));
      respService = RespService.create(socketChannel);
    } catch (Exception e) {
      outputWriter.writeWithNewLine(
          String.format("Failed to reconnect to new leader. %s", e.getMessage()));
      return false;
    }
    respServiceProvider.updateRespService(respService);
    outputWriter.writeWithNewLine(
        String.format(
            "Reconnected to the current Bitflask server leader host [%s] port [%d]. Retry your command.",
            host, port));
    return true;
  }
}

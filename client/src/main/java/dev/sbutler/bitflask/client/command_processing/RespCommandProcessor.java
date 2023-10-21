package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.resp.messages.RespResponse;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.types.RespElement;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import java.io.IOException;

/** Handles processing commands via communication with a remote server using the RESP protocol. */
public class RespCommandProcessor {

  private final Provider<RespService> respServiceProvider;

  @Inject
  public RespCommandProcessor(Provider<RespService> respServiceProvider) {
    this.respServiceProvider = respServiceProvider;
  }

  public RespResponse sendRequest(RespRequest request) throws ProcessingException {
    writeCommand(request);
    return readResponse();
  }

  private void writeCommand(RespRequest request) throws ProcessingException {
    try {
      respServiceProvider.get().write(request.getAsRespArray());
    } catch (IOException e) {
      throw new ProcessingException("Failed to write command", e);
    }
  }

  private RespResponse readResponse() throws ProcessingException {
    try {
      RespElement respElement = respServiceProvider.get().read();
      if (respElement.isRespError()) {
        throw new ProcessingException(
            String.format(
                "Server responded with unrecoverable error. %s", respElement.getAsRespError()));
      } else if (!respElement.isRespArray()) {
        throw new ProcessingException(
            String.format("The server did not return an expected RespElement. [%s]", respElement));
      }
      return RespResponse.createFromRespArray(respElement.getAsRespArray());
    } catch (IOException e) {
      throw new ProcessingException("Failed to read response", e);
    }
  }
}

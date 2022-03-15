package dev.sbutler.bitflask.server.client_handling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ClientRequestModuleTest {

  private final ClientRequestModule clientRequestModule = new ClientRequestModule();

  @Test
  void provideClientRequestHandler() {
    ClientRequestHandlerImpl clientRequestHandlerImpl = mock(ClientRequestHandlerImpl.class);
    ClientRequestHandler clientRequestHandler = clientRequestModule.provideClientRequestHandler(
        clientRequestHandlerImpl);
    assertEquals(clientRequestHandlerImpl, clientRequestHandler);
    assertInstanceOf(ClientRequestHandler.class, clientRequestHandler);
  }
}

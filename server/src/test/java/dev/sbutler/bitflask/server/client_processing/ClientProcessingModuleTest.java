package dev.sbutler.bitflask.server.client_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ClientProcessingModuleTest {

  private final ClientProcessingModule clientProcessingModule = new ClientProcessingModule();

  @Test
  void provideClientRequestHandler() {
    ClientMessageProcessorImpl clientMessageProcessorImpl = mock(ClientMessageProcessorImpl.class);
    ClientMessageProcessor clientMessageProcessor = clientProcessingModule.provideClientRequestHandler(
        clientMessageProcessorImpl);
    assertEquals(clientMessageProcessorImpl, clientMessageProcessor);
    assertInstanceOf(ClientMessageProcessorImpl.class, clientMessageProcessor);
  }
}

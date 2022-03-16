package dev.sbutler.bitflask.server.client_handling;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.server.client_handling.connection.ClientConnectionManager;
import dev.sbutler.bitflask.server.client_handling.processing.ClientMessageProcessor;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
public class ClientRequestHandlerImplTest {

  @InjectMocks
  ClientRequestHandlerImpl clientRequestHandler;

  @Mock
  ClientConnectionManager clientConnectionManager;
  @Mock
  ClientMessageProcessor clientMessageProcessor;

  @BeforeEach
  void beforeEach() {
    clientRequestHandler.logger = mock(Logger.class);
  }

  @Test
  void run() throws IOException {
    doReturn(false).when(clientMessageProcessor).processNextMessage();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> clientRequestHandler.run());
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void close() throws IOException {
    clientRequestHandler.close();
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void close_IOException() throws IOException {
    doThrow(new IOException("Test: failure to close socket")).when(clientConnectionManager).close();

    clientRequestHandler.close();

    verify(clientConnectionManager, times(1)).close();
  }
}

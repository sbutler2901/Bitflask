package dev.sbutler.bitflask.server.client_processing;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.server.client_connection.ClientConnectionManager;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientRequestHandlerTest {

  @InjectMocks
  ClientRequestHandler clientRequestHandler;

  @Mock
  ClientConnectionManager clientConnectionManager;
  @Mock
  ClientMessageProcessor clientMessageProcessor;

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

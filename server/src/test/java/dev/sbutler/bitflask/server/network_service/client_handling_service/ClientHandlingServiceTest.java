package dev.sbutler.bitflask.server.network_service.client_handling_service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.server.network_service.client_handling_service.connection.ClientConnectionManager;
import dev.sbutler.bitflask.server.network_service.client_handling_service.processing.ClientMessageProcessor;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientHandlingServiceTest {

  @InjectMocks
  ClientHandlingService clientHandlingService;

  @Mock
  ClientConnectionManager clientConnectionManager;
  @Mock
  ClientMessageProcessor clientMessageProcessor;

  @Test
  void run() throws IOException {
    doReturn(false).when(clientMessageProcessor).processNextMessage();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> clientHandlingService.run());
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void run_close_IOException() throws IOException {
    doReturn(false).when(clientMessageProcessor).processNextMessage();
    doThrow(IOException.class).when(clientConnectionManager).close();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> clientHandlingService.run());
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void close() throws IOException {
    clientHandlingService.close();
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void close_IOException() throws IOException {
    doThrow(new IOException("Test: failure to close socket")).when(clientConnectionManager).close();

    assertThrows(IOException.class, () -> clientHandlingService.close());

    verify(clientConnectionManager, times(1)).close();
  }
}

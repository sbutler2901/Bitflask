package dev.sbutler.bitflask.server.network_service.client_handling_service;

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
  void run() throws Exception {
    // Arrange
    doReturn(false).when(clientMessageProcessor).processNextMessage();
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> clientHandlingService.run());
    // Assert
    verify(clientMessageProcessor, times(1)).processNextMessage();
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void run_runtimeException() throws Exception {
    // Arrange
    doThrow(RuntimeException.class).when(clientMessageProcessor).processNextMessage();
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> clientHandlingService.run());
    // Assert
    verify(clientMessageProcessor, times(1)).processNextMessage();
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void close() throws Exception {
    // Act
    clientHandlingService.close();
    // Assert
    verify(clientConnectionManager, times(1)).close();
  }

  @Test
  void close_IOException() throws Exception {
    // Arrange
    doReturn(false).when(clientMessageProcessor).processNextMessage();
    doThrow(IOException.class).when(clientConnectionManager).close();
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(1000), () -> clientHandlingService.run());
    // Assert
    verify(clientMessageProcessor, times(1)).processNextMessage();
    verify(clientConnectionManager, times(1)).close();
  }
}

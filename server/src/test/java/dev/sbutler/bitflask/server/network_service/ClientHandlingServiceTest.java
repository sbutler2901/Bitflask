package dev.sbutler.bitflask.server.network_service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.testing.TestingExecutors;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientHandlingServiceTest {

  private ClientHandlingService clientHandlingService;
  private ClientMessageProcessor clientMessageProcessor;

  @SuppressWarnings("UnstableApiUsage")
  @BeforeEach
  void beforeEach() {
    clientMessageProcessor = mock(ClientMessageProcessor.class);
    clientHandlingService = new ClientHandlingService(
        TestingExecutors.sameThreadScheduledExecutor(),
        clientMessageProcessor);
  }

  @Test
  void run() throws Exception {
    // Arrange
    when(clientMessageProcessor.isOpen()).thenReturn(true);
    when(clientMessageProcessor.processNextMessage()).thenReturn(false);
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(clientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
    // Assert
    verify(clientMessageProcessor, times(1)).isOpen();
    verify(clientMessageProcessor, times(1)).processNextMessage();
    verify(clientMessageProcessor, times(1)).close();
  }

  @Test
  void run_runtimeException() throws Exception {
    // Arrange
    when(clientMessageProcessor.isOpen()).thenReturn(true);
    doThrow(RuntimeException.class).when(clientMessageProcessor).processNextMessage();
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(clientHandlingService));
    serviceManager.startAsync();
    // Assert
    verify(clientMessageProcessor, times(1)).isOpen();
    verify(clientMessageProcessor, times(1)).processNextMessage();
    verify(clientMessageProcessor, times(1)).close();
  }
}

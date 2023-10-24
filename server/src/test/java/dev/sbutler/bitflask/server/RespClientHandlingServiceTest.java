package dev.sbutler.bitflask.server;

import static com.google.common.util.concurrent.testing.TestingExecutors.sameThreadScheduledExecutor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ServiceManager;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.server.RespClientHandlingService.Factory;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RespClientHandlingServiceTest {

  private RespClientHandlingService respClientHandlingService;

  @Mock private RespClientMessageProcessor respClientMessageProcessor;

  @SuppressWarnings("UnstableApiUsage")
  @Spy
  ListeningExecutorService listeningExecutorService = sameThreadScheduledExecutor();

  @BeforeEach
  void beforeEach() throws Exception {
    RespClientMessageProcessor.Factory clientMessageProcessorFactory =
        mock(RespClientMessageProcessor.Factory.class);
    when(clientMessageProcessorFactory.create(any())).thenReturn(respClientMessageProcessor);
    RespClientHandlingService.Factory clientHandlingServiceFactory =
        new Factory(listeningExecutorService, clientMessageProcessorFactory);

    try (MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      respServiceMockedStatic
          .when(() -> RespService.create(any()))
          .thenReturn(mock(RespService.class));
      respClientHandlingService = clientHandlingServiceFactory.create(mock(SocketChannel.class));
    }
  }

  @Test
  void run_clientMessageProcessor_isOpenTerminates() throws Exception {
    // Arrange
    when(respClientMessageProcessor.isOpen()).thenReturn(false);
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(respClientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
    // Assert
    verify(respClientMessageProcessor, times(1)).isOpen();
    verify(respClientMessageProcessor, times(0)).processNextMessage();
    verify(respClientMessageProcessor, times(1)).close();
  }

  @Test
  void run_clientMessageProcessor_processNextMessageTerminates() throws Exception {
    // Arrange
    when(respClientMessageProcessor.isOpen()).thenReturn(true);
    when(respClientMessageProcessor.processNextMessage()).thenReturn(true).thenReturn(false);
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(respClientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
    // Assert
    verify(respClientMessageProcessor, times(2)).isOpen();
    verify(respClientMessageProcessor, times(2)).processNextMessage();
    verify(respClientMessageProcessor, times(1)).close();
  }

  @Test
  void run_runtimeException() throws Exception {
    // Arrange
    when(respClientMessageProcessor.isOpen()).thenReturn(true);
    doThrow(RuntimeException.class).when(respClientMessageProcessor).processNextMessage();
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(respClientHandlingService));
    serviceManager.startAsync();
    // Assert
    verify(respClientMessageProcessor, times(1)).isOpen();
    verify(respClientMessageProcessor, times(1)).processNextMessage();
    verify(respClientMessageProcessor, times(1)).close();
  }

  @Test
  void doStop_IOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(respClientMessageProcessor).close();
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(respClientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
  }

  @Test
  void doStop_Exception() throws Exception {
    // Arrange
    doThrow(Exception.class).when(respClientMessageProcessor).close();
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(respClientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
  }
}

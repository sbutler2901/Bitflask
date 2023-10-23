package dev.sbutler.bitflask.server.network_resp;

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
import dev.sbutler.bitflask.server.network_resp.ClientHandlingService.Factory;
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
public class ClientHandlingServiceTest {

  private ClientHandlingService clientHandlingService;

  @Mock private ClientMessageProcessor clientMessageProcessor;

  @SuppressWarnings("UnstableApiUsage")
  @Spy
  ListeningExecutorService listeningExecutorService = sameThreadScheduledExecutor();

  @BeforeEach
  void beforeEach() throws Exception {
    ClientMessageProcessor.Factory clientMessageProcessorFactory =
        mock(ClientMessageProcessor.Factory.class);
    when(clientMessageProcessorFactory.create(any())).thenReturn(clientMessageProcessor);
    ClientHandlingService.Factory clientHandlingServiceFactory =
        new Factory(listeningExecutorService, clientMessageProcessorFactory);

    try (MockedStatic<RespService> respServiceMockedStatic = mockStatic(RespService.class)) {
      respServiceMockedStatic
          .when(() -> RespService.create(any()))
          .thenReturn(mock(RespService.class));
      clientHandlingService = clientHandlingServiceFactory.create(mock(SocketChannel.class));
    }
  }

  @Test
  void run_clientMessageProcessor_isOpenTerminates() throws Exception {
    // Arrange
    when(clientMessageProcessor.isOpen()).thenReturn(false);
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(clientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
    // Assert
    verify(clientMessageProcessor, times(1)).isOpen();
    verify(clientMessageProcessor, times(0)).processNextMessage();
    verify(clientMessageProcessor, times(1)).close();
  }

  @Test
  void run_clientMessageProcessor_processNextMessageTerminates() throws Exception {
    // Arrange
    when(clientMessageProcessor.isOpen()).thenReturn(true);
    when(clientMessageProcessor.processNextMessage()).thenReturn(true).thenReturn(false);
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(clientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
    // Assert
    verify(clientMessageProcessor, times(2)).isOpen();
    verify(clientMessageProcessor, times(2)).processNextMessage();
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

  @Test
  void doStop_IOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(clientMessageProcessor).close();
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(clientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
  }

  @Test
  void doStop_Exception() throws Exception {
    // Arrange
    doThrow(Exception.class).when(clientMessageProcessor).close();
    // Act
    ServiceManager serviceManager = new ServiceManager(ImmutableSet.of(clientHandlingService));
    serviceManager.startAsync();
    serviceManager.stopAsync().awaitStopped(Duration.ofMillis(100));
  }
}

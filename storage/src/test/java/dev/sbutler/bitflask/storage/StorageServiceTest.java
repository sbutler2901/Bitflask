package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ServiceManager.Listener;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.common.dispatcher.DispatcherSubmission;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

  StorageService storageService;
  @Mock
  SegmentManagerService segmentManagerService;
  @Mock
  StorageCommandDispatcher storageCommandDispatcher;
  @Mock
  CommandMapper commandMapper;

  ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

  @Test
  void doStart() {
    try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction =
        mockConstruction(ServiceManager.class)) {
      // Arrange
      storageService = new StorageService(segmentManagerService, storageCommandDispatcher,
          commandMapper);
      ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
      doReturn(serviceManager).when(serviceManager).startAsync();
      doReturn(serviceManager).when(serviceManager).stopAsync();
      // Act
      scheduledExecutorService.schedule(() -> storageService.stopAsync(), 5, TimeUnit.SECONDS);
      storageService.startAsync();
      // Assert
      verify(serviceManager, times(1)).startAsync();
      verify(serviceManager, times(1)).awaitHealthy();
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  void run() throws Exception {
    try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction =
        mockConstruction(ServiceManager.class)) {
      // Arrange
      storageService = new StorageService(segmentManagerService,
          storageCommandDispatcher, commandMapper);
      ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
      doReturn(serviceManager).when(serviceManager).startAsync();
      doReturn(serviceManager).when(serviceManager).stopAsync();
      String key = "key";
      ReadDTO dto = new ReadDTO(key);
      SettableFuture<StorageResponse> submissionResponseFuture = mock(SettableFuture.class);
      DispatcherSubmission<StorageCommandDTO, StorageResponse> submission =
          new DispatcherSubmission<>(dto, submissionResponseFuture);
      doReturn(submission).when(storageCommandDispatcher).poll(anyLong(), any(TimeUnit.class));
      StorageCommand command = mock(StorageCommand.class);
      ListenableFuture<StorageResponse> commandResponseFuture = mock(ListenableFuture.class);
      doReturn(commandResponseFuture).when(command).execute();
      doReturn(command).when(commandMapper).mapToCommand(any(StorageCommandDTO.class));
      // Act
      storageService.startAsync().awaitRunning();
      scheduledExecutorService.schedule(() -> storageService.stopAsync(), 5, TimeUnit.SECONDS);
      storageService.run();
      // Assert
      verify(commandMapper, atLeastOnce()).mapToCommand(dto);
      verify(command, atLeastOnce()).execute();
      verify(submissionResponseFuture, atLeastOnce()).setFuture(commandResponseFuture);
    }
  }

  @Test
  void run_exception() throws Exception {
    try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction =
        mockConstruction(ServiceManager.class)) {
      // Arrange
      storageService = new StorageService(segmentManagerService,
          storageCommandDispatcher, commandMapper);
      ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
      doReturn(serviceManager).when(serviceManager).startAsync();
      doReturn(serviceManager).when(serviceManager).stopAsync();
      RuntimeException e = new RuntimeException("test");
      doThrow(e).when(storageCommandDispatcher)
          .poll(anyLong(), any(TimeUnit.class));
      // Act
      storageService.startAsync().awaitRunning();
      storageService.run();
      // Assert
      assertEquals(e, storageService.failureCause());
    }
  }

  @Test
  void doStop() {
    AtomicReference<ServiceManager> serviceManagerAtomicReference = new AtomicReference<>();
    ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);
    try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction = mockConstruction(
        ServiceManager.class, (mock, context) -> {
          doReturn(mock).when(mock).stopAsync();
          serviceManagerAtomicReference.set(mock);
        })) {
      // Arrange
      storageService = new StorageService(segmentManagerService,
          storageCommandDispatcher, commandMapper);
      ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
      doReturn(serviceManager).when(serviceManager).startAsync();
      doReturn(serviceManager).when(serviceManager).stopAsync();
      // Act
      scheduledExecutorService.schedule(() -> storageService.stopAsync(), 5, TimeUnit.SECONDS);
      storageService.startAsync();
      verify(serviceManagerAtomicReference.get()).addListener(listenerArgumentCaptor.capture(),
          any());
      // Assert
      Listener serviceManagerListener = listenerArgumentCaptor.getValue();
      serviceManagerListener.healthy();
      serviceManagerListener.stopped();
      serviceManagerListener.failure(storageService);
    }
  }

  @Test
  void doStop_TimeoutException() throws Exception {
    try (MockedConstruction<ServiceManager> serviceManagerMockedConstruction =
        mockConstruction(ServiceManager.class)) {
      // Arrange
      storageService = new StorageService(segmentManagerService,
          storageCommandDispatcher, commandMapper);
      ServiceManager serviceManager = serviceManagerMockedConstruction.constructed().get(0);
      doReturn(serviceManager).when(serviceManager).startAsync();
      doReturn(serviceManager).when(serviceManager).stopAsync();
      doThrow(TimeoutException.class).when(serviceManager).awaitStopped(anyLong(), any());
      // Act
      scheduledExecutorService.schedule(() -> storageService.stopAsync(), 5, TimeUnit.SECONDS);
      storageService.startAsync().awaitRunning();
      storageService.run();
    }
  }
}

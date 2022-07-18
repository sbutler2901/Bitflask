package dev.sbutler.bitflask.server.command_processing_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandProcessingServiceTest {

  @InjectMocks
  CommandProcessingService commandProcessingService;
  @Mock
  ExecutorService executorService;
  @Mock
  StorageCommandDispatcher storageCommandDispatcher;

  @Test
  void ping() {
    try (MockedConstruction<PingCommand> pingMockedConstruction = mockConstruction(
        PingCommand.class)) {
      // Arrange
      ImmutableList<String> message = ImmutableList.of("ping");
      // Act
      commandProcessingService.processMessage(message);
      // Assert
      assertEquals(1, pingMockedConstruction.constructed().size());
      PingCommand pingCommand = pingMockedConstruction.constructed().get(0);
      verify(pingCommand, times(1)).execute();
    }
  }

  @Test
  void ping_invalid() {
    // Arrange
    ImmutableList<String> message = ImmutableList.of("ping", "invalid");
    // Act
    ListenableFuture<String> responseFuture = commandProcessingService.processMessage(message);
    // Assert
    assertTrue(responseFuture.isDone());
    try {
      responseFuture.get();
      fail("ExecutionException should have been thrown");
    } catch (ExecutionException e) {
      assertInstanceOf(IllegalArgumentException.class, e.getCause());
    } catch (InterruptedException e) {
      fail("invalid exception");
    }
  }

  @Test
  void get() {
    try (MockedConstruction<GetCommand> getMockedConstruction = mockConstruction(
        GetCommand.class)) {
      // Arrange
      ImmutableList<String> message = ImmutableList.of("get", "key");
      // Act
      commandProcessingService.processMessage(message);
      // Assert
      assertEquals(1, getMockedConstruction.constructed().size());
      GetCommand getCommand = getMockedConstruction.constructed().get(0);
      verify(getCommand, times(1)).execute();
    }
  }

  @Test
  void get_invalid() {
    // Arrange
    ImmutableList<String> message = ImmutableList.of("get", "key", "invalid");
    // Act
    ListenableFuture<String> responseFuture = commandProcessingService.processMessage(message);
    // Assert
    assertTrue(responseFuture.isDone());
    try {
      responseFuture.get();
      fail("ExecutionException should have been thrown");
    } catch (ExecutionException e) {
      assertInstanceOf(IllegalArgumentException.class, e.getCause());
    } catch (InterruptedException e) {
      fail("invalid exception");
    }
  }

  @Test
  void set() {
    try (MockedConstruction<SetCommand> getMockedConstruction = mockConstruction(
        SetCommand.class)) {
      // Arrange
      ImmutableList<String> message = ImmutableList.of("set", "key", "value");
      // Act
      commandProcessingService.processMessage(message);
      // Assert
      assertEquals(1, getMockedConstruction.constructed().size());
      SetCommand setCommand = getMockedConstruction.constructed().get(0);
      verify(setCommand, times(1)).execute();
    }
  }

  @Test
  void set_invalid() {
    // Arrange
    ImmutableList<String> message = ImmutableList.of("set", "key");
    // Act
    ListenableFuture<String> responseFuture = commandProcessingService.processMessage(message);
    // Assert
    assertTrue(responseFuture.isDone());
    try {
      responseFuture.get();
      fail("ExecutionException should have been thrown");
    } catch (ExecutionException e) {
      assertInstanceOf(IllegalArgumentException.class, e.getCause());
    } catch (InterruptedException e) {
      fail("invalid exception");
    }
  }
}

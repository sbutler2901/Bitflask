package dev.sbutler.bitflask.server.command_processing_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.StorageService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandProcessingServiceTest {

  @InjectMocks
  CommandProcessingService commandProcessor;

  @Mock
  StorageService storageService;

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_get() throws ExecutionException, InterruptedException {
    // Value found
    String key0 = "test0", value0 = "value";
    ServerCommand command0 = new ServerCommand(Command.GET, List.of(key0));
    ListenableFuture<Optional<String>> readFuture0 = mock(ListenableFuture.class);

    doReturn(Optional.of(value0)).when(readFuture0).get();
    doReturn(readFuture0).when(storageService).read(key0);

    assertEquals(value0, commandProcessor.processServerCommand(command0));

    // Value not found
    String key1 = "test1";
    ServerCommand command1 = new ServerCommand(Command.GET, List.of(key1));
    ListenableFuture<Optional<String>> readFuture1 = mock(ListenableFuture.class);

    doReturn(Optional.empty()).when(readFuture1).get();
    doReturn(readFuture1).when(storageService).read(key1);

    assertTrue(commandProcessor.processServerCommand(command1).contains("not found"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServiceCommand_get_InterruptedException()
      throws ExecutionException, InterruptedException {
    String key = "test";
    ServerCommand command = new ServerCommand(Command.GET, List.of(key));
    ListenableFuture<Optional<String>> readFuture = mock(ListenableFuture.class);

    doThrow(InterruptedException.class).when(readFuture).get();
    doReturn(readFuture).when(storageService).read(key);

    String getResult = commandProcessor.processServerCommand(command);
    assertTrue(getResult.toLowerCase().contains("error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServiceCommand_get_ExecutionException()
      throws ExecutionException, InterruptedException {
    String key = "test";
    ServerCommand command = new ServerCommand(Command.GET, List.of(key));
    ListenableFuture<Optional<String>> readFuture = mock(ListenableFuture.class);

    ExecutionException executionException = new ExecutionException(new IOException("Test error"));
    doThrow(executionException).when(readFuture).get();
    doReturn(readFuture).when(storageService).read(key);

    String getResult = commandProcessor.processServerCommand(command);
    assertTrue(getResult.toLowerCase().contains("error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_set() {
    String key = "key", value = "value";
    ServerCommand command = new ServerCommand(Command.SET, List.of(key, value));
    ListenableFuture<Void> writeFuture = mock(ListenableFuture.class);

    doReturn(writeFuture).when(storageService).write(key, value);

    assertEquals("OK", commandProcessor.processServerCommand(command));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_set_InterruptedException()
      throws ExecutionException, InterruptedException {
    String key = "key", value = "value";
    ServerCommand command = new ServerCommand(Command.SET, List.of(key, value));
    ListenableFuture<Void> writeFuture = mock(ListenableFuture.class);

    doThrow(InterruptedException.class).when(writeFuture).get();
    doReturn(writeFuture).when(storageService).write(key, value);

    String setResult = commandProcessor.processServerCommand(command);
    assertTrue(setResult.toLowerCase().contains("error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_set_ExecutionException()
      throws ExecutionException, InterruptedException {
    String key = "key", value = "value";
    ServerCommand command = new ServerCommand(Command.SET, List.of(key, value));
    ListenableFuture<Void> writeFuture = mock(ListenableFuture.class);

    ExecutionException executionException = new ExecutionException(new IOException("Test error"));
    doThrow(executionException).when(writeFuture).get();
    doReturn(writeFuture).when(storageService).write(key, value);

    String setResult = commandProcessor.processServerCommand(command);
    assertTrue(setResult.toLowerCase().contains("error"));
  }

  @Test
  void processServerCommand_ping() {
    ServerCommand command = new ServerCommand(Command.PING, null);
    assertEquals("pong", commandProcessor.processServerCommand(command));
  }
}

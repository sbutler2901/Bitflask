package dev.sbutler.bitflask.server.command_processing_service;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandProcessingServiceTest {
  /*
  @InjectMocks
  CommandProcessingService commandProcessor;

  @Mock
  StorageService storageService;

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_get() throws ExecutionException, InterruptedException {
    // Value found
    String key0 = "test0", value0 = "value";
    GetCommand command0 = mock(GetCommand.class);
    doReturn(key0).when(command0).getKey();
    ListenableFuture<Optional<String>> readFuture0 = mock(ListenableFuture.class);

    doReturn(Optional.of(value0)).when(readFuture0).get();
    doReturn(readFuture0).when(storageService).read(key0);

//    assertEquals(value0, commandProcessor.processServerCommand(command0));

    // Value not found
    String key1 = "test1";
    GetCommand command1 = mock(GetCommand.class);
    doReturn(key1).when(command1).getKey();
    ListenableFuture<Optional<String>> readFuture1 = mock(ListenableFuture.class);

    doReturn(Optional.empty()).when(readFuture1).get();
    doReturn(readFuture1).when(storageService).read(key1);

//    assertTrue(commandProcessor.processServerCommand(command1).contains("not found"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServiceCommand_get_InterruptedException()
      throws ExecutionException, InterruptedException {
    String key = "test";
    GetCommand command = mock(GetCommand.class);
    doReturn(key).when(command).getKey();
    ListenableFuture<StorageResponse> readFuture = mock(ListenableFuture.class);

    doThrow(InterruptedException.class).when(readFuture).get();
    doReturn(readFuture).when(storageService).read(key);

//    String getResult = commandProcessor.processServerCommand(command);
//    assertTrue(getResult.toLowerCase().contains("error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServiceCommand_get_ExecutionException()
      throws ExecutionException, InterruptedException {
    String key = "test";
    GetCommand command = mock(GetCommand.class);
    doReturn(key).when(command).getKey();
    ListenableFuture<StorageResponse> readFuture = mock(ListenableFuture.class);

    ExecutionException executionException = new ExecutionException(new IOException("Test error"));
    doThrow(executionException).when(readFuture).get();
    doReturn(readFuture).when(storageService).read(key);

//    String getResult = commandProcessor.processServerCommand(command);
//    assertTrue(getResult.toLowerCase().contains("error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_set() {
    String key = "key", value = "value";
    SetCommand command = mock(SetCommand.class);
    doReturn(key).when(command).getKey();
    doReturn(value).when(command).getValue();
    ListenableFuture<StorageResponse> writeFuture = mock(ListenableFuture.class);

    doReturn(writeFuture).when(storageService).write(key, value);

//    assertEquals("OK", commandProcessor.processServerCommand(command));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_set_InterruptedException()
      throws ExecutionException, InterruptedException {
    String key = "key", value = "value";
    SetCommand command = mock(SetCommand.class);
    doReturn(key).when(command).getKey();
    doReturn(value).when(command).getValue();
    ListenableFuture<StorageResponse> writeFuture = mock(ListenableFuture.class);

    doThrow(InterruptedException.class).when(writeFuture).get();
    doReturn(writeFuture).when(storageService).write(key, value);

//    String setResult = commandProcessor.processServerCommand(command);
//    assertTrue(setResult.toLowerCase().contains("error"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void processServerCommand_set_ExecutionException()
      throws ExecutionException, InterruptedException {
    String key = "key", value = "value";
    SetCommand command = mock(SetCommand.class);
    doReturn(key).when(command).getKey();
    doReturn(value).when(command).getValue();
    ListenableFuture<StorageResponse> writeFuture = mock(ListenableFuture.class);

    ExecutionException executionException = new ExecutionException(new IOException("Test error"));
    doThrow(executionException).when(writeFuture).get();
    doReturn(writeFuture).when(storageService).write(key, value);

//    String setResult = commandProcessor.processServerCommand(command);
//    assertTrue(setResult.toLowerCase().contains("error"));
  }

  @Test
  void processServerCommand_ping() {
    PingCommand command = mock(PingCommand.class);
//    assertEquals("pong", commandProcessor.processServerCommand(command));
  }
  */
}

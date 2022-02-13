package dev.sbutler.bitflask.server.command_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.server.storage.Storage;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandProcessorTest {

  @InjectMocks
  CommandProcessor commandProcessor;

  @Mock
  Storage storage;

  @Test
  void processServerCommand_get() throws IOException {
    String key0 = "test0", value0 = "value";
    ServerCommand command0 = new ServerCommand(Command.GET, List.of(key0));
    when(storage.read(key0)).thenReturn(Optional.of(value0));
    assertEquals(value0, commandProcessor.processServerCommand(command0));

    String key1 = "test1", value1 = "Not Found";
    ServerCommand command1 = new ServerCommand(Command.GET, List.of(key1));
    when(storage.read(key1)).thenReturn(Optional.of(value1));
    assertEquals(value1, commandProcessor.processServerCommand(command1));
  }

  @Test
  void processServerCommand_set() throws IOException {
    String key = "key", value = "value";
    ServerCommand command = new ServerCommand(Command.SET, List.of(key, value));
    assertEquals("OK", commandProcessor.processServerCommand(command));
  }

  @Test
  void processServerCommand_ping() throws IOException {
    ServerCommand command = new ServerCommand(Command.PING, null);
    assertEquals("pong", commandProcessor.processServerCommand(command));
  }
}

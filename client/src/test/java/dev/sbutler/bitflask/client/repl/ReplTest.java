package dev.sbutler.bitflask.client.repl;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import dev.sbutler.bitflask.client.command_processing.CommandProcessor;
import dev.sbutler.bitflask.client.repl.input.InputParser;
import dev.sbutler.bitflask.client.repl.output.OutputWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplTest {

  final ClientCommand exitCommand = new ClientCommand("exit", null);

  @InjectMocks
  Repl repl;
  @Mock
  CommandProcessor commandProcessor;
  @Mock
  InputParser inputParser;
  @Mock
  OutputWriter outputWriter;

  @Test
  void anyCommand_nullInput() {
    doReturn(null, exitCommand).when(inputParser).getNextCommand();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> repl.start());
    verify(outputWriter, times(2)).writeWithNewLine(anyString());
  }

  @Test
  void replCommand_exit() {
    doReturn(exitCommand).when(inputParser).getNextCommand();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> repl.start());
    verify(outputWriter, times(2)).writeWithNewLine(anyString());
  }

  @Test
  void replCommand_help() {
    ClientCommand command = new ClientCommand("help", null);
    doReturn(command, exitCommand).when(inputParser).getNextCommand();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> repl.start());
    verify(outputWriter, times(3)).writeWithNewLine(anyString());
  }

  @Test
  void replCommand_invalidArgs() {
    ClientCommand command = new ClientCommand("help", List.of("invalid-arg"));
    doReturn(command, exitCommand).when(inputParser).getNextCommand();
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> repl.start());
    verify(outputWriter, times(3)).writeWithNewLine(anyString());
  }

  @Test
  void serverCommand() throws IOException {
    ClientCommand command = new ClientCommand("ping", null);
    doReturn(command, exitCommand).when(inputParser).getNextCommand();
    String response = "pong";
    doReturn(response).when(commandProcessor).runCommand(command);
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> repl.start());
    verify(outputWriter, times(1)).writeWithNewLine(response);
  }

  @Test
  void serverCommand_IOException() throws IOException {
    ClientCommand command = new ClientCommand("ping", null);
    doReturn(command).when(inputParser).getNextCommand();
    doThrow(new IOException("Test: commandProcessor")).when(commandProcessor).runCommand(command);
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> repl.start());
    verify(outputWriter, times(3)).writeWithNewLine(anyString());
  }
}

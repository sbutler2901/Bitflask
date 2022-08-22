package dev.sbutler.bitflask.client.client_processing.repl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import dev.sbutler.bitflask.client.command_processing.RespCommandProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplClientProcessorServiceTest {

  //  final ClientCommand exitCommand = new ClientCommand("exit", null);
  final ImmutableList<String> exitCommand = ImmutableList.of("exit");

  @InjectMocks
  ReplClientProcessorService replClientProcessorService;
  @Mock
  RespCommandProcessor respCommandProcessor;
  @Mock
  InputParser inputParser;
  @Mock
  OutputWriter outputWriter;

  @Test
  void anyCommand_nullInput() {
    // Arrange
    doReturn(ImmutableList.of(), exitCommand).when(inputParser).getClientNextInput();
    // Act
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(2)).write(anyString());
  }

  @Test
  void replCommand_exit() {
    // Arrange
    doReturn(exitCommand).when(inputParser).getClientNextInput();
    // Act
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(1)).write(anyString());
  }

  @Test
  void replCommand_help() {
    // Arrange
    ImmutableList<String> command = ImmutableList.of("help");
    doReturn(command, exitCommand).when(inputParser).getClientNextInput();
    // Act
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
    verify(outputWriter, times(2)).write(anyString());
  }

  @Test
  void replCommand_invalidArgs() {
    // Arrange
    ImmutableList<String> command = ImmutableList.of("help", "invalid-arg");
    doReturn(command, exitCommand).when(inputParser).getClientNextInput();
    // Act
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
    verify(outputWriter, times(2)).write(anyString());
  }

  @Test
  void serverCommand() throws ProcessingException {
    // Arrange
    ImmutableList<String> command = ImmutableList.of("ping");
    doReturn(command, exitCommand).when(inputParser).getClientNextInput();
    String response = "pong";
    doReturn(response).when(respCommandProcessor).runCommand(any());
    // Act
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(1)).writeWithNewLine(response);
  }

  @Test
  void serverCommand_IOException() throws ProcessingException {
    // Arrange
    ImmutableList<String> command = ImmutableList.of("ping");
    doReturn(command, exitCommand).when(inputParser).getClientNextInput();
    doThrow(new ProcessingException("Test: commandProcessor")).when(respCommandProcessor)
        .runCommand(any());
    // Act
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
    verify(outputWriter, times(1)).write(anyString());
  }

  @Test
  void triggerShutdown() {
    // Act
    replClientProcessorService.triggerShutdown();
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(0)).writeWithNewLine(anyString());
    verify(outputWriter, times(0)).write(anyString());
  }
}

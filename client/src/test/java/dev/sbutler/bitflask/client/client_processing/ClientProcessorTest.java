package dev.sbutler.bitflask.client.client_processing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplString;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import dev.sbutler.bitflask.client.command_processing.RemoteCommandProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientProcessorTest {

  @InjectMocks
  ClientProcessor clientProcessor;
  @Mock
  RemoteCommandProcessor remoteCommandProcessor;
  @Mock
  OutputWriter outputWriter;

  @Test
  void localCommand_exit() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("exit"));
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertFalse(shouldContinue);
  }

  @Test
  void localCommand_help() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("help"));
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertTrue(shouldContinue);
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }

  @Test
  void remoteCommand() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("get"),
        new ReplString("test"));
    doReturn("test result").when(remoteCommandProcessor).runCommand(any());
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertTrue(shouldContinue);
    verify(remoteCommandProcessor, times(1)).runCommand(any());
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }

  @Test
  void remoteCommand_ProcessingException() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("get"),
        new ReplString("test"));
    doThrow(ProcessingException.class).when(remoteCommandProcessor).runCommand(any());
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertFalse(shouldContinue);
    verify(remoteCommandProcessor, times(1)).runCommand(any());
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }

  @Test
  void emptyInput() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of();
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertTrue(shouldContinue);
    verify(remoteCommandProcessor, times(0)).runCommand(any());
    verify(outputWriter, times(0)).writeWithNewLine(anyString());
    verify(outputWriter, times(0)).write(anyString());
  }

}

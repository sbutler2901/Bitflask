package dev.sbutler.bitflask.client.client_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplString;
import dev.sbutler.bitflask.client.command_processing.ProcessingException;
import dev.sbutler.bitflask.client.command_processing.RespCommandProcessor;
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
  RespCommandProcessor respCommandProcessor;
  @Mock
  OutputWriter outputWriter;

  @Test
  void localCommand_exit() {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("exit"));
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertThat(shouldContinue).isFalse();
  }

  @Test
  void localCommand_help() {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("help"));
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertThat(shouldContinue).isTrue();
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }

  @Test
  void remoteCommand() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("get"),
        new ReplString("test"));
    when(respCommandProcessor.runCommand(any())).thenReturn("test result");
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertThat(shouldContinue).isTrue();
    verify(respCommandProcessor, times(1)).runCommand(any());
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }

  @Test
  void remoteCommand_ProcessingException() throws Exception {
    // Arrange
    ImmutableList<ReplElement> clientInput = ImmutableList.of(new ReplString("get"),
        new ReplString("test"));
    when(respCommandProcessor.runCommand(any())).thenThrow(ProcessingException.class);
    // Act
    boolean shouldContinue = clientProcessor.processClientInput(clientInput);
    // Assert
    assertThat(shouldContinue).isFalse();
    verify(respCommandProcessor, times(1)).runCommand(any());
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }
}

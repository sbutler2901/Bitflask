package dev.sbutler.bitflask.client.command_processing;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Exit;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Help;
import org.junit.jupiter.api.Test;

public class LocalCommandTest {

  @Test
  void help() {
    // Arrange
    OutputWriter outputWriter = mock(OutputWriter.class);
    Help help = new Help(outputWriter);
    // Act
    help.execute();
    // Assert
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }

  @Test
  void exit() {
    // Arrange
    Exit exit = new Exit();
    // Act / Assert
    exit.execute();
  }
}

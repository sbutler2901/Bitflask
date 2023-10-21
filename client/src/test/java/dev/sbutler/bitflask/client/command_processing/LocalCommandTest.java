package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Exit;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Help;
import dev.sbutler.bitflask.client.command_processing.LocalCommand.Unknown;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LocalCommand}. */
public class LocalCommandTest {

  @Test
  public void help() {
    OutputWriter outputWriter = mock(OutputWriter.class);
    Help help = new Help(outputWriter);

    boolean shouldContinue = help.execute();

    assertThat(shouldContinue).isTrue();
    verify(outputWriter, times(1)).writeWithNewLine("I can't help you.");
  }

  @Test
  public void exit() {
    Exit exit = new Exit();

    boolean shouldContinue = exit.execute();

    assertThat(shouldContinue).isFalse();
  }

  @Test
  public void unknown() {
    OutputWriter outputWriter = mock(OutputWriter.class);
    Unknown unknown = new Unknown(outputWriter, "unknown");

    boolean shouldContinue = unknown.execute();

    assertThat(shouldContinue).isTrue();
    verify(outputWriter, times(1)).writeWithNewLine(anyString());
  }
}

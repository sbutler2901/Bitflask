package dev.sbutler.bitflask.client.client_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;
import org.junit.jupiter.api.Test;

public class ClientProcessingModuleTest {

  private final ClientProcessingModule clientProcessingModule = new ClientProcessingModule();

  @Test
  void provideOutputWriter() {
    StdoutOutputWriter stdoutOutputWriter = mock(StdoutOutputWriter.class);
    OutputWriter outputWriter = clientProcessingModule.provideOutputWriter(stdoutOutputWriter);
    assertEquals(stdoutOutputWriter, outputWriter);
    assertInstanceOf(StdoutOutputWriter.class, outputWriter);
  }
}
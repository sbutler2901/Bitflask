package dev.sbutler.bitflask.client.client_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.input.StdinInputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;
import org.junit.jupiter.api.Test;

public class ClientProcessingModuleTest {

  private final ClientProcessingModule clientProcessingModule = new ClientProcessingModule();

  @Test
  void provideClientProcessor() {
    ReplClientProcessorService replClientProcessorService = mock(ReplClientProcessorService.class);
    ClientProcessorService clientProcessorService = clientProcessingModule.provideClientProcessor(
        replClientProcessorService);
    assertEquals(replClientProcessorService, clientProcessorService);
    assertInstanceOf(ReplClientProcessorService.class, clientProcessorService);
  }

  @Test
  void provideInputParser() {
    StdinInputParser stdinInputParser = mock(StdinInputParser.class);
    InputParser inputParser = clientProcessingModule.provideInputParser(stdinInputParser);
    assertEquals(stdinInputParser, inputParser);
    assertInstanceOf(StdinInputParser.class, inputParser);
  }

  @Test
  void provideOutputWriter() {
    StdoutOutputWriter stdoutOutputWriter = mock(StdoutOutputWriter.class);
    OutputWriter outputWriter = clientProcessingModule.provideOutputWriter(stdoutOutputWriter);
    assertEquals(stdoutOutputWriter, outputWriter);
    assertInstanceOf(StdoutOutputWriter.class, outputWriter);
  }
}

package dev.sbutler.bitflask.server.command_processing_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class CommandProcessingModuleTest {

  private final CommandProcessingModule commandProcessingModule = new CommandProcessingModule();

  @Test
  void provideCommandProcessor() {
    CommandProcessorImpl commandProcessorImpl = mock(CommandProcessorImpl.class);
    CommandProcessor commandProcessor = commandProcessingModule.provideCommandProcessor(
        commandProcessorImpl);
    assertEquals(commandProcessorImpl, commandProcessor);
    assertInstanceOf(CommandProcessorImpl.class, commandProcessor);
  }
}

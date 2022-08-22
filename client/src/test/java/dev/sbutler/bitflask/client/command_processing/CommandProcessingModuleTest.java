package dev.sbutler.bitflask.client.command_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class CommandProcessingModuleTest {

  private final CommandProcessingModule commandProcessingModule = new CommandProcessingModule();

  @Test
  void provideCommandProcessor() {
    RespCommandProcessor respCommandProcessor = mock(RespCommandProcessor.class);
    RemoteCommandProcessor commandProcessor = commandProcessingModule.provideCommandProcessor(
        respCommandProcessor);
    assertEquals(respCommandProcessor, commandProcessor);
    assertInstanceOf(RespCommandProcessor.class, commandProcessor);
  }

}

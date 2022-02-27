package dev.sbutler.bitflask.client.command_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class CommandProcessingModule extends AbstractModule {

  @Provides
  CommandProcessor provideCommandProcessor(RespCommandProcessor commandProcessor) {
    return commandProcessor;
  }

}

package dev.sbutler.bitflask.server.command_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class CommandProcessingModule extends AbstractModule {

  @Provides
  CommandProcessor provideCommandProcessor(CommandProcessorImpl commandProcessor) {
    return commandProcessor;
  }

}
package dev.sbutler.bitflask.client.command_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class CommandProcessingModule extends AbstractModule {

  @Provides
  RemoteCommandProcessor provideCommandProcessor(RespCommandProcessor commandProcessor) {
    return commandProcessor;
  }

}

package dev.sbutler.bitflask.server.client_handling.processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class ClientProcessingModule extends AbstractModule {

  @Provides
  ClientMessageProcessor provideClientRequestHandler(ClientMessageProcessorImpl clientMessageProcessor) {
    return clientMessageProcessor;
  }
}

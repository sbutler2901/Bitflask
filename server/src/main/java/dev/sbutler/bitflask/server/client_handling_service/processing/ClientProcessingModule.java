package dev.sbutler.bitflask.server.client_handling_service.processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.resp.network.RespNetworkModule;

public class ClientProcessingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    install(new RespNetworkModule());
  }

  @Provides
  ClientMessageProcessor provideClientRequestHandler(
      ClientMessageProcessorImpl clientMessageProcessor) {
    return clientMessageProcessor;
  }
}

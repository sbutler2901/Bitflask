package dev.sbutler.bitflask.server.client_handling;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ClientRequestModule extends AbstractModule {

  @Provides
  @Singleton
  ClientRequestHandler provideClientRequestHandler(ClientRequestHandlerImpl clientRequestHandler) {
    return clientRequestHandler;
  }

}

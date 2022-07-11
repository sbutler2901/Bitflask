package dev.sbutler.bitflask.server.network_service.client_handling_service.processing;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.resp.network.RespNetworkModule;

public class ClientProcessingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    install(new RespNetworkModule());
  }
}

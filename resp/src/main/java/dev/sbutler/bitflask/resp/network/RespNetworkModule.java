package dev.sbutler.bitflask.resp.network;

import com.google.inject.AbstractModule;

public class RespNetworkModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    install(new RespReaderModule());
  }

}

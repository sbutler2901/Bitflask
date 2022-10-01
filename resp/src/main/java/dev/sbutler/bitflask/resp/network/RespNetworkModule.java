package dev.sbutler.bitflask.resp.network;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.resp.network.reader.RespReaderModule;

public class RespNetworkModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    install(new RespReaderModule());
  }

}

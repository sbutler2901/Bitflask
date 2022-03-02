package dev.sbutler.bitflask.resp.network;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.resp.network.reader.RespReaderModule;
import dev.sbutler.bitflask.resp.network.writer.RespWriterModule;

public class RespNetworkModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    install(new RespReaderModule());
    install(new RespWriterModule());
  }

}

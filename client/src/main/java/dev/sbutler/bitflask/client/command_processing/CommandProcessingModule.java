package dev.sbutler.bitflask.client.command_processing;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.resp.utilities.RespReader;
import dev.sbutler.bitflask.resp.utilities.RespWriter;
import java.io.InputStream;
import java.io.OutputStream;

public class CommandProcessingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(CommandProcessor.class).to(RespCommandProcessor.class);
  }

  @Provides
  RespReader providedRespReader(InputStream inputStream) {
    return new RespReader(inputStream);
  }

  @Provides
  RespWriter provideRespWriter(OutputStream outputStream) {
    return new RespWriter(outputStream);
  }

}

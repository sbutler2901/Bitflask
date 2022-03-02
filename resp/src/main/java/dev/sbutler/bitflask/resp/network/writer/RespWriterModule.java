package dev.sbutler.bitflask.resp.network.writer;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class RespWriterModule extends AbstractModule {

  @Provides
  RespWriter provideRespWriter(RespWriterImpl respWriter) {
    return respWriter;
  }
}

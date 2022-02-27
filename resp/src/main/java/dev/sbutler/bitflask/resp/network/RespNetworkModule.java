package dev.sbutler.bitflask.resp.network;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.reader.RespReaderImpl;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.network.writer.RespWriterImpl;

public class RespNetworkModule extends AbstractModule {

  @Provides
  @Singleton
  RespReader provideRespReader(RespReaderImpl respReader) {
    return respReader;
  }

  @Provides
  @Singleton
  RespWriter provideRespWriter(RespWriterImpl respWriter) {
    return respWriter;
  }
}

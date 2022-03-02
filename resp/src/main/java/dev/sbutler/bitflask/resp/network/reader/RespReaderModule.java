package dev.sbutler.bitflask.resp.network.reader;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RespReaderModule extends AbstractModule {

  @Provides
  RespReader provideRespReader(RespReaderImpl respReader) {
    return respReader;
  }

  @Provides
  BufferedReader provideBufferedReader(InputStreamReader inputStreamReader) {
    return new BufferedReader(inputStreamReader);
  }

  @Provides
  InputStreamReader provideInputStreamReader(InputStream inputStream) {
    return new InputStreamReader(inputStream);
  }
}

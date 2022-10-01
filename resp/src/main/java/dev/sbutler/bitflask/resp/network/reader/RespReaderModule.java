package dev.sbutler.bitflask.resp.network.reader;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RespReaderModule extends AbstractModule {

  @Provides
  @RespReaderBufferedReader
  BufferedReader provideBufferedReader(InputStream inputStream) {
    return new BufferedReader(new InputStreamReader(inputStream));
  }
}

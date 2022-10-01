package dev.sbutler.bitflask.resp.network;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class RespNetworkModule extends AbstractModule {

  @Provides
  RespReader provideRespReader(InputStream inputStream) {
    return new RespReader(new BufferedReader(new InputStreamReader(inputStream)));
  }

  @Provides
  RespWriter provideRespWriter(OutputStream outputStream) {
    return new RespWriter(outputStream);
  }
}

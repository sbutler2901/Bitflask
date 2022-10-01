package dev.sbutler.bitflask.resp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class RespNetworkModuleTest {

  @Test
  void configure() {
    Injector injector = Guice.createInjector(
        new MockStreamProvider(),
        new RespNetworkModule()
    );
    injector.getProvider(RespReader.class);
    injector.getProvider(RespWriter.class);
  }

  @Test
  void provideBufferedReader() {
    try (MockedConstruction<BufferedReader> bufferedReaderMockedConstruction = mockConstruction(
        BufferedReader.class)) {
      RespNetworkModule respNetworkModule = new RespNetworkModule();
      InputStream inputStream = mock(InputStream.class);
      BufferedReader providedBufferedReader = respNetworkModule.provideBufferedReader(inputStream);
      BufferedReader mockedBufferedReader = bufferedReaderMockedConstruction.constructed().get(0);
      assertEquals(mockedBufferedReader, providedBufferedReader);
    }
  }

  static class MockStreamProvider extends AbstractModule {

    @Provides
    InputStream providesInputStream() {
      return mock(InputStream.class);
    }

    @Provides
    OutputStream provideOutputStream() {
      return mock(OutputStream.class);
    }
  }
}

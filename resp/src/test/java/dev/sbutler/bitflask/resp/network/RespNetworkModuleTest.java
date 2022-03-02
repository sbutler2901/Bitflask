package dev.sbutler.bitflask.resp.network;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

public class RespNetworkModuleTest {

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

  @Test
  void configure() {
    Injector injector = Guice.createInjector(
        new MockStreamProvider(),
        new RespNetworkModule()
    );
    injector.getProvider(RespReader.class);
    injector.getProvider(RespWriter.class);
  }
}

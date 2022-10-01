package dev.sbutler.bitflask.resp.network;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

public class RespNetworkModuleTest {

  private final RespNetworkModule respNetworkModule = new RespNetworkModule();

  @Test
  void configure() {
    Injector injector = Guice.createInjector(
        new MockStreamProvider(),
        respNetworkModule
    );
    injector.getProvider(RespReader.class);
    injector.getProvider(RespWriter.class);
  }

  @Test
  void provideRespReader() {
    // Arrange
    InputStream inputStream = mock(InputStream.class);
    // Act
    RespReader respReader = respNetworkModule.provideRespReader(inputStream);
    // Assert
    assertNotNull(respReader);
  }

  @Test
  void provideRespWriter() {
    // Arrange
    OutputStream outputStream = mock(OutputStream.class);
    // Act
    RespWriter respWriter = respNetworkModule.provideRespWriter(outputStream);
    // Assert
    assertNotNull(respWriter);
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

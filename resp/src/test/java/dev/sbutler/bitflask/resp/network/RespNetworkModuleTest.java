package dev.sbutler.bitflask.resp.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import dev.sbutler.bitflask.resp.network.reader.RespReader;
import dev.sbutler.bitflask.resp.network.reader.RespReaderImpl;
import dev.sbutler.bitflask.resp.network.writer.RespWriter;
import dev.sbutler.bitflask.resp.network.writer.RespWriterImpl;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class RespNetworkModuleTest {

  private final RespNetworkModule respNetworkModule = new RespNetworkModule();

  @Test
  void provideRespReader() {
    RespReaderImpl respReaderImpl = mock(RespReaderImpl.class);
    RespReader respReader = respNetworkModule.provideRespReader(respReaderImpl);
    assertEquals(respReaderImpl, respReader);
    assertInstanceOf(RespReaderImpl.class, respReader);
  }

  @Test
  void provideBufferedReader() {
    try (MockedConstruction<BufferedReader> bufferedReaderMockedConstruction = mockConstruction(
        BufferedReader.class)) {
      InputStreamReader inputStreamReader = mock(InputStreamReader.class);
      BufferedReader providedBufferedReader = respNetworkModule.provideBufferedReader(
          inputStreamReader);
      BufferedReader mockedBufferedReader = bufferedReaderMockedConstruction.constructed().get(0);
      assertEquals(mockedBufferedReader, providedBufferedReader);
    }
  }

  @Test
  void provideInputStreamReader() {
    try (MockedConstruction<InputStreamReader> inputStreamReaderMockedConstruction = mockConstruction(
        InputStreamReader.class)) {
      InputStream inputStream = mock(InputStream.class);
      InputStreamReader providedInputStreamReader = respNetworkModule.provideInputStreamReader(
          inputStream);
      InputStreamReader mockedInputStreamReader = inputStreamReaderMockedConstruction.constructed()
          .get(0);
      assertEquals(mockedInputStreamReader, providedInputStreamReader);
    }
  }

  @Test
  void provideRespWriter() {
    RespWriterImpl respWriterImpl = mock(RespWriterImpl.class);
    RespWriter respWriter = respNetworkModule.provideRespWriter(respWriterImpl);
    assertEquals(respWriterImpl, respWriter);
    assertInstanceOf(RespWriterImpl.class, respWriter);
  }

}

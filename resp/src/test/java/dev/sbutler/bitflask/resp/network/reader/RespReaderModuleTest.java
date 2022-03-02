package dev.sbutler.bitflask.resp.network.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class RespReaderModuleTest {

  private final RespReaderModule respReaderModule = new RespReaderModule();

  @Test
  void provideRespReader() {
    RespReaderImpl respReaderImpl = mock(RespReaderImpl.class);
    RespReader respReader = respReaderModule.provideRespReader(respReaderImpl);
    assertEquals(respReaderImpl, respReader);
    assertInstanceOf(RespReaderImpl.class, respReader);
  }

  @Test
  void provideBufferedReader() {
    try (MockedConstruction<BufferedReader> bufferedReaderMockedConstruction = mockConstruction(
        BufferedReader.class)) {
      InputStreamReader inputStreamReader = mock(InputStreamReader.class);
      BufferedReader providedBufferedReader = respReaderModule.provideBufferedReader(
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
      InputStreamReader providedInputStreamReader = respReaderModule.provideInputStreamReader(
          inputStream);
      InputStreamReader mockedInputStreamReader = inputStreamReaderMockedConstruction.constructed()
          .get(0);
      assertEquals(mockedInputStreamReader, providedInputStreamReader);
    }
  }
}

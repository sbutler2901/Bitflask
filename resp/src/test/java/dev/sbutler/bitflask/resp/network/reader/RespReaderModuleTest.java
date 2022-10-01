package dev.sbutler.bitflask.resp.network.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;

import java.io.BufferedReader;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class RespReaderModuleTest {

  private final RespReaderModule respReaderModule = new RespReaderModule();

  @Test
  void provideBufferedReader() {
    try (MockedConstruction<BufferedReader> bufferedReaderMockedConstruction = mockConstruction(
        BufferedReader.class)) {
      InputStream inputStream = mock(InputStream.class);
      BufferedReader providedBufferedReader = respReaderModule.provideBufferedReader(inputStream);
      BufferedReader mockedBufferedReader = bufferedReaderMockedConstruction.constructed().get(0);
      assertEquals(mockedBufferedReader, providedBufferedReader);
    }
  }
}

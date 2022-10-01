package dev.sbutler.bitflask.resp.network;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.io.BufferedOutputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RespWriterTest {

  @InjectMocks
  RespWriter respWriter;

  @Mock
  BufferedOutputStream bufferedOutputStream;

  @Test
  void writeRespType_success() throws IOException {
    RespType<?> toWrite = new RespBulkString("test");
    respWriter.writeRespType(toWrite);
    verify(bufferedOutputStream, times(1)).write(toWrite.getEncodedBytes());
  }

}

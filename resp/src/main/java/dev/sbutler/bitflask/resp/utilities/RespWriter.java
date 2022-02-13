package dev.sbutler.bitflask.resp.utilities;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RespWriter {

  private final BufferedOutputStream bufferedOutputStream;

  public RespWriter(BufferedOutputStream bufferedOutputStream) {
    this.bufferedOutputStream = bufferedOutputStream;
  }

  public RespWriter(OutputStream outputStream) {
    this.bufferedOutputStream = new BufferedOutputStream(outputStream);
  }

  public void writeRespType(RespType<?> respType) throws IOException {
    bufferedOutputStream.write(respType.getEncodedBytes());
    bufferedOutputStream.flush();
  }

}

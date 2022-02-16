package dev.sbutler.bitflask.resp.utilities;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class RespWriter {

  private final OutputStream outputStream;

  public RespWriter(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  public void writeRespType(RespType<?> respType) throws IOException {
    outputStream.write(respType.getEncodedBytes());
    outputStream.flush();
  }

}

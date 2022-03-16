package dev.sbutler.bitflask.resp.network.writer;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.IOException;
import java.io.OutputStream;
import javax.inject.Inject;

class RespWriterImpl implements RespWriter {

  private final OutputStream outputStream;

  @Inject
  RespWriterImpl(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  public void writeRespType(RespType<?> respType) throws IOException {
    outputStream.write(respType.getEncodedBytes());
    outputStream.flush();
  }

}

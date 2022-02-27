package dev.sbutler.bitflask.resp.network.writer;

import com.google.inject.Inject;
import dev.sbutler.bitflask.resp.types.RespType;
import java.io.IOException;
import java.io.OutputStream;

// todo: reduce class visibility
public class RespWriterImpl implements RespWriter {

  private final OutputStream outputStream;

  // todo reduce constructor visibility
  @Inject
  public RespWriterImpl(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  public void writeRespType(RespType<?> respType) throws IOException {
    outputStream.write(respType.getEncodedBytes());
    outputStream.flush();
  }

}

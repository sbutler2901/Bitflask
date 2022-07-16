package dev.sbutler.bitflask.resp.network.writer;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.IOException;

public interface RespWriter {

  /**
   * Writes the provided RespType to the underlying output-stream
   *
   * @param respType the data to be written
   * @throws IOException if a general failure occurs while reading
   */
  void writeRespType(RespType<?> respType) throws IOException;
}

package dev.sbutler.bitflask.resp.network.reader;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;

public interface RespReader {

  /**
   * Reads the next RespType from the underlying input-stream
   *
   * @return the read RespType
   * @throws EOFException      if the underlying input-stream is closed
   * @throws ProtocolException if the read input data is malformed
   * @throws IOException       if a general failure occurs while reading
   */
  RespType<?> readNextRespType() throws IOException;
}

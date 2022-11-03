package dev.sbutler.bitflask.resp.network;

import dev.sbutler.bitflask.resp.types.RespElement;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Handles writing RESP data types to an underlying {@link OutputStream}.
 *
 * <p>this class does not handle lifecycle management of the provided OutputStream, such as closing
 * it.
 */
public class RespWriter {

  private final OutputStream outputStream;

  public RespWriter(OutputStream outputStream) {
    this.outputStream = outputStream;
  }

  /**
   * Writes the provided RespElement to the underlying output-stream
   *
   * @param respElement the data to be written
   * @throws IOException if a general failure occurs while reading
   */
  public void writeRespElement(RespElement respElement) throws IOException {
    outputStream.write(respElement.getEncodedBytes());
    outputStream.flush();
  }

}

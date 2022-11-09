package dev.sbutler.bitflask.resp.network;

import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.resp.types.RespSimpleString;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles reading RESP data types from an underlying {@link java.io.InputStream} via a
 * {@link Reader}.
 *
 * <p>This class does not handle lifecycle management of the provided Reader, such as
 * closing it.
 */
public class RespReader {

  private final Reader reader;

  public RespReader(Reader reader) {
    this.reader = reader;
  }

  /**
   * Reads the next RespElement from the underlying input-stream
   *
   * @return the read RespElement
   * @throws EOFException      if the underlying input-stream is closed
   * @throws ProtocolException if the read input data is malformed
   * @throws IOException       if a general failure occurs while reading
   */
  public RespElement readNextRespElement() throws IOException {
    int code = reader.read();
    if (code == -1) {
      // TODO: consider wrapping result with optional
      throw new EOFException("Could not parse next RespElement");
    }
    return switch (code) {
      case RespSimpleString.TYPE_PREFIX -> readRespSimpleString();
      case RespBulkString.TYPE_PREFIX -> readRespBulkString();
      case RespInteger.TYPE_PREFIX -> readRespInteger();
      case RespError.TYPE_PREFIX -> readRespError();
      case RespArray.TYPE_PREFIX -> readRespArray();
      default -> throw new ProtocolException("RespElement code not recognized");
    };
  }

  private RespSimpleString readRespSimpleString()
      throws IOException {
    String value = readLine();
    return new RespSimpleString(value);
  }

  private RespBulkString readRespBulkString()
      throws IOException {
    int length = Integer.parseInt(readLine());
    if (length == RespBulkString.NULL_STRING_LENGTH) {
      return new RespBulkString(null);
    }
    String readValue = readLine();
    if (readValue.length() != length) {
      throw new ProtocolException("RespBulkString value length didn't match provided length");
    }
    return new RespBulkString(readValue);
  }

  private RespInteger readRespInteger() throws IOException {
    int value = Integer.parseInt(readLine());
    return new RespInteger(value);
  }

  private RespError readRespError() throws IOException {
    String value = readLine();
    return new RespError(value);
  }

  private RespArray readRespArray() throws IOException {
    int numItems = Integer.parseInt(readLine());
    if (numItems == RespArray.NULL_ARRAY_LENGTH) {
      return new RespArray(null);
    }
    List<RespElement> respArrayValues = new ArrayList<>();
    for (int i = 0; i < numItems; i++) {
      respArrayValues.add(readNextRespElement());
    }
    return new RespArray(respArrayValues);
  }

  /**
   * Reads a line terminated with CRLF.
   *
   * <p>Other line terminators will be read like normal. Reaching EOF will also be considered
   * terminating the read line
   */
  private String readLine() throws IOException {
    StringBuilder builder = new StringBuilder();
    int read;
    boolean crLastRead = false;
    while ((read = reader.read()) != -1) {
      char c = (char) read;
      if (crLastRead) {
        if (c == '\n') {
          // EOL
          break;
        }
        // Add skipped CR since not EOL
        builder.append('\r');
        // Another CR NOT found, can add like normal
        if (c != '\r') {
          builder.append(c);
          crLastRead = false;
        }
      } else if (c == '\r') {
        crLastRead = true;
      } else {
        builder.append(c);
      }
    }
    return builder.toString();
  }
}

package bitflask.resp;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;

public class RespUtils {

  private RespUtils() {
    throw new AssertionError();
  }

  public static RespType<?> readNextRespType(BufferedReader bufferedReader) throws IOException {
    int code = bufferedReader.read();
    if (code == -1) {
      throw new EOFException("Could not parse RespType");
    }
    return switch (code) {
      case RespSimpleString.TYPE_PREFIX -> readRespSimpleString(bufferedReader);
      case RespBulkString.TYPE_PREFIX -> readRespBulkString(bufferedReader);
      case RespInteger.TYPE_PREFIX -> readRespInteger(bufferedReader);
      case RespError.TYPE_PREFIX -> readRespError(bufferedReader);
      case RespArray.TYPE_PREFIX -> readRespArray(bufferedReader);
      default -> throw new ProtocolException("RespType code not recognized");
    };
  }

  private static RespSimpleString readRespSimpleString(BufferedReader bufferedReader)
      throws IOException {
    String value = bufferedReader.readLine();
    return new RespSimpleString(value);
  }

  private static RespBulkString readRespBulkString(BufferedReader bufferedReader)
      throws IOException {
    int length = Integer.parseInt(bufferedReader.readLine());
    if (length == RespBulkString.NULL_STRING_LENGTH) {
      return new RespBulkString(null);
    }
    String readValue = bufferedReader.readLine();
    if (readValue.length() != length) {
      throw new ProtocolException("RespBulkString value length didn't match provided length");
    }
    String value = bufferedReader.readLine();
    return new RespBulkString(value);
  }

  private static RespInteger readRespInteger(BufferedReader bufferedReader) throws IOException {
    int value = Integer.parseInt(bufferedReader.readLine());
    return new RespInteger(value);
  }

  private static RespError readRespError(BufferedReader bufferedReader) throws IOException {
    String value = bufferedReader.readLine();
    return new RespError(value);
  }

  private static RespArray readRespArray(BufferedReader bufferedReader) throws IOException {
    int numItems = Integer.parseInt(bufferedReader.readLine());
    if (numItems == RespArray.NULL_ARRAY_LENGTH) {
      return new RespArray(null);
    }
    List<RespType<?>> respArrayValues = new ArrayList<>();
    for (int i = 0; i < numItems; i++) {
      respArrayValues.add(readNextRespType(bufferedReader));
    }
    return new RespArray(respArrayValues);
  }
}

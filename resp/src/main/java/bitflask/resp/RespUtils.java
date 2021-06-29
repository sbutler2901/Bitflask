package bitflask.resp;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.net.ProtocolException;

public class RespUtils {
  private RespUtils() {
    throw new AssertionError();
  }

  public static RespType readNextRespType(BufferedReader bufferedReader) throws IOException {
    int code = bufferedReader.read();
    if (code == -1) {
      throw new EOFException("Could not parse RespType");
    }
    switch (code) {
      case RespSimpleString.TYPE_PREFIX:
        return new RespSimpleString(bufferedReader);
      case RespBulkString.TYPE_PREFIX:
        return new RespBulkString(bufferedReader);
      case RespInteger.TYPE_PREFIX:
        return new RespInteger(bufferedReader);
      case RespError.TYPE_PREFIX:
        return new RespError(bufferedReader);
      case RespArray.TYPE_PREFIX:
        return new RespArray(bufferedReader);
      default:
        throw new ProtocolException("RespType code not recognized");
    }
  }
}

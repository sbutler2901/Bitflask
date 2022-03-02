package dev.sbutler.bitflask.resp.network.reader;

import com.google.inject.Inject;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespError;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.resp.types.RespSimpleString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.List;

class RespReaderImpl implements RespReader {

  private final BufferedReader bufferedReader;

  @Inject
  RespReaderImpl(@RespReaderBufferedReader BufferedReader reader) {
    this.bufferedReader = reader;
  }

  public RespType<?> readNextRespType() throws IOException {
    int code = bufferedReader.read();
    if (code == -1) {
      throw new EOFException("Could not parse RespType");
    }
    return switch (code) {
      case RespSimpleString.TYPE_PREFIX -> readRespSimpleString();
      case RespBulkString.TYPE_PREFIX -> readRespBulkString();
      case RespInteger.TYPE_PREFIX -> readRespInteger();
      case RespError.TYPE_PREFIX -> readRespError();
      case RespArray.TYPE_PREFIX -> readRespArray();
      default -> throw new ProtocolException("RespType code not recognized");
    };
  }

  private RespSimpleString readRespSimpleString()
      throws IOException {
    String value = bufferedReader.readLine();
    return new RespSimpleString(value);
  }

  private RespBulkString readRespBulkString()
      throws IOException {
    int length = Integer.parseInt(bufferedReader.readLine());
    if (length == RespBulkString.NULL_STRING_LENGTH) {
      return new RespBulkString(null);
    }
    String readValue = bufferedReader.readLine();
    if (readValue.length() != length) {
      throw new ProtocolException("RespBulkString value length didn't match provided length");
    }
    return new RespBulkString(readValue);
  }

  private RespInteger readRespInteger() throws IOException {
    int value = Integer.parseInt(bufferedReader.readLine());
    return new RespInteger(value);
  }

  private RespError readRespError() throws IOException {
    String value = bufferedReader.readLine();
    return new RespError(value);
  }

  private RespArray readRespArray() throws IOException {
    int numItems = Integer.parseInt(bufferedReader.readLine());
    if (numItems == RespArray.NULL_ARRAY_LENGTH) {
      return new RespArray(null);
    }
    List<RespType<?>> respArrayValues = new ArrayList<>();
    for (int i = 0; i < numItems; i++) {
      respArrayValues.add(readNextRespType());
    }
    return new RespArray(respArrayValues);
  }

}

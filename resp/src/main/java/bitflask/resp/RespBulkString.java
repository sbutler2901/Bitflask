package bitflask.resp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public class RespBulkString implements RespType<String> {

  public static final char TYPE_PREFIX = '$';
  public static final long NULL_STRING_LENGTH = -1;

  private final String value;

  public RespBulkString(BufferedReader bufferedReader) throws IOException {
    int length = Integer.parseInt(bufferedReader.readLine());
    if (length == NULL_STRING_LENGTH) {
      value = null;
      return;
    }
    String readValue = bufferedReader.readLine();
    if (readValue.length() != length) {
      throw new IllegalArgumentException("Value length didn't match provided length");
    }
    this.value = readValue;
  }

  public RespBulkString(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void write(BufferedOutputStream bufferedOutputStream) throws IOException {
    bufferedOutputStream.write(TYPE_PREFIX);
    if (value == null) {
      bufferedOutputStream.write(String.valueOf(NULL_STRING_LENGTH).getBytes(ENCODED_CHARSET));
    } else {
      bufferedOutputStream.write(String.valueOf(value.length()).getBytes(ENCODED_CHARSET));
      bufferedOutputStream.write(CRLF);
      bufferedOutputStream.write(value.getBytes(ENCODED_CHARSET));
    }
    bufferedOutputStream.write(CRLF);
  }

  @Override
  public String toString() {
    return value;
  }
}

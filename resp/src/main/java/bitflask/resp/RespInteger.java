package bitflask.resp;

import static bitflask.resp.RespConstants.CRLF;
import static bitflask.resp.RespConstants.ENCODED_CHARSET;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;

public class RespInteger implements RespType<Integer> {

  public static final char TYPE_PREFIX = ':';

  private final int value;

  public RespInteger(BufferedReader bufferedReader) throws IOException {
    this.value = Integer.parseInt(bufferedReader.readLine());
  }

  public RespInteger(int value) {
    this.value = value;
  }

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public void write(BufferedOutputStream bufferedOutputStream) throws IOException {
    bufferedOutputStream.write(TYPE_PREFIX);
    bufferedOutputStream.write(String.valueOf(value).getBytes(ENCODED_CHARSET));
    bufferedOutputStream.write(CRLF);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}

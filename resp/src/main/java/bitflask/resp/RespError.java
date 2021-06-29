package bitflask.resp;

import static bitflask.resp.RespConstants.CRLF;
import static bitflask.resp.RespConstants.ENCODED_CHARSET;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;

public class RespError implements RespType<String> {

  public static final char TYPE_PREFIX = '-';

  private final String value;

  public RespError(BufferedReader bufferedReader) throws IOException {
    this.value = bufferedReader.readLine();
  }

  public RespError(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public void write(BufferedOutputStream bufferedOutputStream) throws IOException {
    bufferedOutputStream.write(TYPE_PREFIX);
    bufferedOutputStream.write(value.getBytes(ENCODED_CHARSET));
    bufferedOutputStream.write(CRLF);
  }

  @Override
  public String toString() {
    return value;
  }
}

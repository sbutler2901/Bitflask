package bitflask.resp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class RespType<T> {

  public static final char CR = '\r';
  public static final char LF = '\n';
  public static final String CRLF = new String(new char[]{CR, LF});
  public static final Charset ENCODED_CHARSET = StandardCharsets.UTF_8;

  public abstract byte[] getEncodedBytes();

  public abstract String getEncodedString();

  public abstract T getDecodedValue();
}

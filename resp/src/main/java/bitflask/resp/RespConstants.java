package bitflask.resp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RespConstants {
  private RespConstants() {
    throw new AssertionError();
  }

  public static final char CR  = '\r';
  public static final char LF = '\n';
  public static final byte[] CRLF = new byte[]{CR, LF};
  public static final Charset ENCODED_CHARSET = StandardCharsets.UTF_8;
}

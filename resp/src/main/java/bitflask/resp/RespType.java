package bitflask.resp;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface RespType<T> {
  char CR = '\r';
  char LF = '\n';
  String CRLF = new String(new char[]{CR, LF});
  Charset ENCODED_CHARSET = StandardCharsets.UTF_8;

  byte[] getEncodedBytes();

  String getEncodedString();

  T getDecodedValue();
}

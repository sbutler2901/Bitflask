package bitflask.resp;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface RespType<T> {
  char CR = '\r';
  char LF = '\n';
  byte[] CRLF = new byte[]{CR, LF};
  Charset ENCODED_CHARSET = StandardCharsets.UTF_8;

  T getValue();
  void write(BufferedOutputStream bufferedOutputStream) throws IOException;
}

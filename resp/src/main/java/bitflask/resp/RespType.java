package bitflask.resp;

import java.io.BufferedOutputStream;
import java.io.IOException;

public interface RespType<T> {
  T getValue();
  void write(BufferedOutputStream bufferedOutputStream) throws IOException;
}

package bitflask.resp;

import static bitflask.resp.RespConstants.CRLF;
import static bitflask.resp.RespConstants.ENCODED_CHARSET;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RespArray implements RespType<List<RespType<?>>> {

  public static final char TYPE_PREFIX = '*';

  private final List<RespType<?>> value;

  public RespArray(BufferedReader bufferedReader) throws IOException {
    this.value = new ArrayList<>();
    int numElements = Integer.parseInt(bufferedReader.readLine());
    for (int i = 0; i < numElements; i++) {
      this.value.add(RespUtils.readNextRespType(bufferedReader));
    }
  }

  public RespArray(List<RespType<?>> value) {
    this.value = value;
  }

  @Override
  public List<RespType<?>> getValue() {
    return value;
  }

  @Override
  public void write(BufferedOutputStream bufferedOutputStream) throws IOException {
    bufferedOutputStream.write(TYPE_PREFIX);
    bufferedOutputStream.write(String.valueOf(value.size()).getBytes(ENCODED_CHARSET));
    bufferedOutputStream.write(CRLF);
    for (RespType<?> respType : value) {
      respType.write(bufferedOutputStream);
    }
  }

  @Override
  public String toString() {
    StringBuilder content = new StringBuilder("[");
    for (int i = 0; i < value.size(); i++) {
      String decodedString = value.get(i).toString();
      content.append(decodedString);
      if (i < value.size() - 1) {
        content.append(", ");
      }
    }
    content.append("]");
    return content.toString();
  }
}

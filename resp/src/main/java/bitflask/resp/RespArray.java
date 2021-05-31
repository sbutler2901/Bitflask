package bitflask.resp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class RespArray extends RespType<List<RespType>> {

  public static final char TYPE_PREFIX = '*';
  public static final String NULL_STRING_LENGTH = "-1";

  private final List<RespType> decodedValue;
  private final String encodedString;
  private final byte[] encodedBytes;

  public RespArray(List<RespType> entries) {
    if (entries == null) {
      this.decodedValue = null;
      this.encodedString = TYPE_PREFIX + NULL_STRING_LENGTH + CRLF;
    } else {
      StringBuilder encodedString = new StringBuilder();

      encodedString.append(TYPE_PREFIX);
      encodedString.append(entries.size());
      encodedString.append(CRLF);

      entries.forEach(entry -> encodedString.append(entry.getEncodedString()));

      this.decodedValue = entries;
      this.encodedString = encodedString.toString();
    }

    this.encodedBytes = encodedString.getBytes(ENCODED_CHARSET);
  }

  public RespArray(byte[] encodedBytes) {
    if (encodedBytes[0] != TYPE_PREFIX) {
      throw new IllegalArgumentException("Invalid byte array");
    }

    if (encodedBytes[1] == '-') {
      this.decodedValue = null;
    } else {
      this.decodedValue = new ArrayList<>();
      int length = 0;
      int index = 1;

      while (encodedBytes[index] != CR) {
        length = (length * 10) + (encodedBytes[index] - '0');
        index++;
      }
      index += 2;

      int endIndex = encodedBytes.length;
      for (int i = 0; i < length; i++) {
        switch (encodedBytes[index]) {
          case RespInteger.TYPE_PREFIX:
            this.decodedValue
                .add(new RespInteger(Arrays.copyOfRange(encodedBytes, index, endIndex)));
            break;
          case RespError.TYPE_PREFIX:
            this.decodedValue.add(new RespError(Arrays.copyOfRange(encodedBytes, index, endIndex)));
            break;
          case RespBulkString.TYPE_PREFIX:
            this.decodedValue
                .add(new RespBulkString(Arrays.copyOfRange(encodedBytes, index, endIndex)));
            break;
          case RespArray.TYPE_PREFIX:
            this.decodedValue.add(new RespArray(Arrays.copyOfRange(encodedBytes, index, endIndex)));
            break;
          case RespSimpleString.TYPE_PREFIX:
            this.decodedValue
                .add(new RespSimpleString(Arrays.copyOfRange(encodedBytes, index, endIndex)));
            break;
          default:
            throw new IllegalArgumentException("Bad input");
        }
        index += this.decodedValue.get(this.decodedValue.size() - 1).getEncodedBytes().length;
      }
    }

    this.encodedString = new String(encodedBytes, ENCODED_CHARSET);
    this.encodedBytes = encodedBytes;
  }

  @Override
  public byte[] getEncodedBytes() {
    return encodedBytes;
  }

  @Override
  public String getEncodedString() {
    return encodedString;
  }

  @Override
  public List<RespType> getDecodedValue() {
    return decodedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RespArray respArray = (RespArray) o;
    return Objects.equals(decodedValue, respArray.decodedValue) && encodedString
        .equals(respArray.encodedString) && Arrays
        .equals(encodedBytes, respArray.encodedBytes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(decodedValue, encodedString);
    result = 31 * result + Arrays.hashCode(encodedBytes);
    return result;
  }
}

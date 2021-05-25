package bitflask.resp;

import java.util.Arrays;
import java.util.Objects;

public class RespInteger extends RespType<Integer> {

  public static final char TYPE_PREFIX = ':';

  private final Integer decodedValue;
  private final String encodedString;
  private final byte[] encodedBytes;

  RespInteger(int decodedValue) {
    this.decodedValue = decodedValue;
    this.encodedString = TYPE_PREFIX + "" + decodedValue + CRLF;
    this.encodedBytes = this.encodedString.getBytes(ENCODED_CHARSET);
  }

  RespInteger(byte[] encodedBytes) {
    if (encodedBytes.length <= 0) {
      throw new IllegalArgumentException("Empty byte array");
    }
    if (encodedBytes[0] != TYPE_PREFIX) {
      throw new IllegalArgumentException("Invalid byte array");
    }

    int index = 1;
    while (index < encodedBytes.length && encodedBytes[index] != CR) {
      index++;
    }

    this.decodedValue = Integer.parseInt(new String(encodedBytes, 1, index - 1));
    this.encodedString = TYPE_PREFIX + "" + decodedValue + CRLF;
    this.encodedBytes = this.encodedString.getBytes(ENCODED_CHARSET);
  }

  @Override
  byte[] getEncodedBytes() {
    return encodedBytes;
  }

  @Override
  String getEncodedString() {
    return encodedString;
  }

  @Override
  Integer getDecodedValue() {
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
    RespInteger that = (RespInteger) o;
    return decodedValue.equals(that.decodedValue) && encodedString.equals(that.encodedString)
        && Arrays.equals(encodedBytes, that.encodedBytes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(decodedValue, encodedString);
    result = 31 * result + Arrays.hashCode(encodedBytes);
    return result;
  }
}

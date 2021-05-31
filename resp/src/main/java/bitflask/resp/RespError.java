package bitflask.resp;

import java.util.Arrays;
import java.util.Objects;
import lombok.NonNull;

public class RespError extends RespType<String> {

  public static final char TYPE_PREFIX = '-';

  private final String decodedValue;
  private final String encodedString;
  private final byte[] encodedBytes;

  RespError(@NonNull String decodedValue) {
    this.decodedValue = decodedValue;
    this.encodedString = TYPE_PREFIX + decodedValue + CRLF;
    this.encodedBytes = encodedString.getBytes(ENCODED_CHARSET);
  }

  RespError(byte[] encodedBytes) {
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

    this.decodedValue = new String(encodedBytes, 1, index - 1);
    this.encodedString = TYPE_PREFIX + decodedValue + CRLF;
    this.encodedBytes = this.encodedString.getBytes(ENCODED_CHARSET);
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
  public String getDecodedValue() {
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
    RespError respError = (RespError) o;
    return decodedValue.equals(respError.decodedValue) && encodedString
        .equals(respError.encodedString) && Arrays
        .equals(encodedBytes, respError.encodedBytes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(decodedValue, encodedString);
    result = 31 * result + Arrays.hashCode(encodedBytes);
    return result;
  }
}

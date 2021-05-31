package bitflask.resp;

import java.util.Arrays;
import java.util.Objects;

public class RespBulkString extends RespType<String> {

  public static final char TYPE_PREFIX = '$';
  public static final String NULL_STRING_LENGTH = "-1";

  private final String decodedValue;
  private final String encodedString;
  private final byte[] encodedBytes;

  public RespBulkString(String decodedValue) {
    this.decodedValue = decodedValue;
    this.encodedString = decodedValue == null
        ? TYPE_PREFIX + NULL_STRING_LENGTH + CRLF
        : TYPE_PREFIX + "" + decodedValue.length() + CRLF + decodedValue + CRLF;
    this.encodedBytes = this.encodedString.getBytes(ENCODED_CHARSET);
  }

  public RespBulkString(byte[] encodedBytes) {
    if (encodedBytes.length <= 0) {
      throw new IllegalArgumentException("Empty byte array");
    }
    if (encodedBytes[0] != TYPE_PREFIX) {
      throw new IllegalArgumentException("Invalid byte array");
    }

    if (encodedBytes[1] == '-') {
      this.decodedValue = null;
      this.encodedString = TYPE_PREFIX + NULL_STRING_LENGTH + CRLF;
    } else {
      int length = 0;
      int index = 1;

      while (encodedBytes[index] != CR) {
        length = (length * 10) + (encodedBytes[index] - '0');
        index++;
      }
      index += 2; // Set to string start

      if (length == 0) {
        this.decodedValue = "";
        this.encodedString = TYPE_PREFIX + "0" + CRLF + CRLF;
      } else {
        this.decodedValue = new String(encodedBytes, index, length);
        this.encodedString = TYPE_PREFIX + "" + length + CRLF + this.decodedValue + CRLF;
      }
    }

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
    RespBulkString that = (RespBulkString) o;
    return Objects.equals(decodedValue, that.decodedValue) && encodedString
        .equals(that.encodedString) && Arrays.equals(encodedBytes, that.encodedBytes);
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(decodedValue, encodedString);
    result = 31 * result + Arrays.hashCode(encodedBytes);
    return result;
  }
}

package bitflask.resp;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RespArray extends RespType<List<RespType<?>>> {

  static final char TYPE_PREFIX = '*';
  static final long NULL_ARRAY_LENGTH = -1;

  private final List<RespType<?>> value;

  public RespArray(List<RespType<?>> value) {
    this.value = value;
  }

  @Override
  public List<RespType<?>> getValue() {
    return value;
  }

  @Override
  public byte[] getEncodedBytes() {
    byte[] encodedValueBytes;
    if (value == null) {
      encodedValueBytes = String.valueOf(NULL_ARRAY_LENGTH).getBytes(RespType.ENCODED_CHARSET);
    } else {
      encodedValueBytes = convertNonNullValueToBytes();
    }
    return RespType.getEncodedBytesFromValueBytes(encodedValueBytes, TYPE_PREFIX);
  }

  private byte[] convertNonNullValueToBytes() {
    if (value.size() == 0) {
      return new byte[]{'0'};
    }

    int valuesEncodedBytesTotalLength = 0;
    List<byte[]> valuesEncodedList = new ArrayList<>();
    for (RespType<?> respType : value) {
      byte[] currentRespTypeEncodedBytes = respType.getEncodedBytes();
      valuesEncodedList.add(currentRespTypeEncodedBytes);
      valuesEncodedBytesTotalLength += currentRespTypeEncodedBytes.length;
    }

    byte[] valueLengthBytes = String.valueOf(value.size()).getBytes(RespType.ENCODED_CHARSET);
    int encodedValueBytesNeededLength = 2 + valueLengthBytes.length + valuesEncodedBytesTotalLength;

    byte[] encodedValueBytes = new byte[encodedValueBytesNeededLength];
    System.arraycopy(valueLengthBytes, 0, encodedValueBytes, 0, valueLengthBytes.length);
    encodedValueBytes[valueLengthBytes.length] = RespType.CR;
    encodedValueBytes[valueLengthBytes.length + 1] = RespType.LF;

    int destPosition = valueLengthBytes.length + 2;
    for (byte[] encodedValue : valuesEncodedList) {
      System.arraycopy(encodedValue, 0, encodedValueBytes, destPosition, encodedValue.length);
      destPosition += encodedValue.length;
    }

    return encodedValueBytes;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RespArray respArray = (RespArray) o;
    return Objects.equals(getValue(), respArray.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getValue());
  }
}

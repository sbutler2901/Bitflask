package dev.sbutler.bitflask.resp.types;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RespArray extends RespElement {

  public static final char TYPE_PREFIX = '*';
  public static final long NULL_ARRAY_LENGTH = -1;

  private final List<RespElement> value;

  public RespArray(List<RespElement> value) {
    this.value = value;
  }

  public List<RespElement> getValue() {
    return value;
  }

  @Override
  public byte[] getEncodedBytes() {
    if (value == null) {
      return getEncodedBytesFromValueBytes(
          String.valueOf(NULL_ARRAY_LENGTH).getBytes(ENCODED_CHARSET),
          TYPE_PREFIX
      );
    } else if (value.size() == 0) {
      return getEncodedBytesFromValueBytes(new byte[]{'0'}, TYPE_PREFIX);
    } else {
      return encodedNonEmptyValueToBytes();
    }
  }

  private byte[] encodedNonEmptyValueToBytes() {
    // Generate value list as encoded bytes and total length;
    int valuesEncodedBytesTotalLength = 0;
    List<byte[]> valuesEncodedList = new ArrayList<>();
    for (RespElement respElement : value) {
      byte[] currentRespElementEncodedBytes = respElement.getEncodedBytes();
      valuesEncodedList.add(currentRespElementEncodedBytes);
      valuesEncodedBytesTotalLength += currentRespElementEncodedBytes.length;
    }

    // Generate bytes for number of items in array
    byte[] valueLengthBytes = String.valueOf(value.size()).getBytes(ENCODED_CHARSET);

    // Initialize array to hold final encoded bytes
    int encodedBytesNeededLength = 1 + valueLengthBytes.length + 2 + valuesEncodedBytesTotalLength;
    byte[] encodedBytes = new byte[encodedBytesNeededLength];

    // Initialize encoding prefix
    encodedBytes[0] = TYPE_PREFIX;
    System.arraycopy(valueLengthBytes, 0, encodedBytes, 1, valueLengthBytes.length);
    encodedBytes[valueLengthBytes.length + 1] = CR;
    encodedBytes[valueLengthBytes.length + 2] = LF;

    // Add encoded values to final coded bytes
    int destPosition = valueLengthBytes.length + 3;
    for (byte[] encodedValue : valuesEncodedList) {
      System.arraycopy(encodedValue, 0, encodedBytes, destPosition, encodedValue.length);
      destPosition += encodedValue.length;
    }

    return encodedBytes;
  }

  public int size() {
    return value.size();
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

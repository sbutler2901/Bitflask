package bitflask.resp;

public class RespBulkString extends RespType<String> {

  public static final char TYPE_PREFIX = '$';
  public static final long NULL_STRING_LENGTH = -1;

  private final String value;

  public RespBulkString(String value) {
    this.value = value;
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public byte[] getEncodedBytes() {
    byte[] encodedValueBytes;
    if (value == null) {
      encodedValueBytes = String.valueOf(NULL_STRING_LENGTH).getBytes(RespType.ENCODED_CHARSET);
    } else {
      encodedValueBytes = convertNonNullValueToBytes();
    }
    return RespType.getEncodedBytesFromValueBytes(encodedValueBytes, TYPE_PREFIX);
  }

  private byte[] convertNonNullValueToBytes() {
    byte[] valueBytes = value.getBytes(RespType.ENCODED_CHARSET);
    byte[] valueLengthBytes = String.valueOf(value.length()).getBytes(RespType.ENCODED_CHARSET);
    int encodedValueBytesNeededLength = 2 + valueLengthBytes.length + value.length();

    byte[] encodedValueBytes = new byte[encodedValueBytesNeededLength];
    System.arraycopy(valueLengthBytes, 0, encodedValueBytes, 0, valueLengthBytes.length);
    encodedValueBytes[valueLengthBytes.length] = RespType.CR;
    encodedValueBytes[valueLengthBytes.length + 1] = RespType.LF;
    System.arraycopy(valueBytes, 0, encodedValueBytes, valueLengthBytes.length + 2,
        valueBytes.length);

    return encodedValueBytes;
  }

  @Override
  public String toString() {
    return value;
  }
}

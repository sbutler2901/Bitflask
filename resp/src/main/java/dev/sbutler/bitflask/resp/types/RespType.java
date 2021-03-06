package dev.sbutler.bitflask.resp.types;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class RespType<T> {

  public static final char CR = '\r';
  public static final char LF = '\n';
  public static final Charset ENCODED_CHARSET = StandardCharsets.UTF_8;

  protected static byte[] getEncodedBytesFromValueBytes(byte[] value, char typePrefix) {
    byte[] encodedBytes = new byte[3 + value.length];
    encodedBytes[0] = (byte) typePrefix;
    System.arraycopy(value, 0, encodedBytes, 1, value.length);
    encodedBytes[1 + value.length] = RespType.CR;
    encodedBytes[2 + value.length] = RespType.LF;
    return encodedBytes;
  }

  public abstract T getValue();

  public abstract byte[] getEncodedBytes();
}

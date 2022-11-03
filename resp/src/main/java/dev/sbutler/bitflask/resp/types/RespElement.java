package dev.sbutler.bitflask.resp.types;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public abstract class RespElement {

  public static final char CR = '\r';
  public static final char LF = '\n';
  public static final Charset ENCODED_CHARSET = StandardCharsets.UTF_8;

  protected static byte[] getEncodedBytesFromValueBytes(byte[] value, char typePrefix) {
    byte[] encodedBytes = new byte[3 + value.length];
    encodedBytes[0] = (byte) typePrefix;
    System.arraycopy(value, 0, encodedBytes, 1, value.length);
    encodedBytes[1 + value.length] = RespElement.CR;
    encodedBytes[2 + value.length] = RespElement.LF;
    return encodedBytes;
  }

  public abstract byte[] getEncodedBytes();

  public boolean isRespSimpleString() {
    return this instanceof RespSimpleString;
  }

  public boolean isRespBulkString() {
    return this instanceof RespBulkString;
  }

  public boolean isRespInteger() {
    return this instanceof RespInteger;
  }

  public boolean isRespError() {
    return this instanceof RespError;
  }

  public boolean isRespArray() {
    return this instanceof RespArray;
  }

  public RespSimpleString getAsRespSimpleString() {
    if (isRespSimpleString()) {
      return (RespSimpleString) this;
    }
    throw new IllegalStateException(
        String.format("Not a RespSimpleString found [%s]", this.getClass().getSimpleName()));
  }

  public RespBulkString getAsRespBulkString() {
    if (isRespBulkString()) {
      return (RespBulkString) this;
    }
    throw new IllegalStateException(
        String.format("Not a RespBulkString found [%s]", this.getClass().getSimpleName()));
  }

  public RespInteger getAsRespInteger() {
    if (isRespInteger()) {
      return (RespInteger) this;
    }
    throw new IllegalStateException(
        String.format("Not a RespInteger found [%s]", this.getClass().getSimpleName()));
  }

  public RespError getAsRespError() {
    if (isRespError()) {
      return (RespError) this;
    }
    throw new IllegalStateException(
        String.format("Not a RespError found [%s]", this.getClass().getSimpleName()));
  }

  public RespArray getAsRespArray() {
    if (isRespArray()) {
      return (RespArray) this;
    }
    throw new IllegalStateException(
        String.format("Not a RespArray found [%s]", this.getClass().getSimpleName()));
  }
}

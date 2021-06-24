package bitflask.resp;

public final class RespUtils {
  public static RespType<?> from(byte[] encodedBytes) {
    switch (encodedBytes[0]) {
      case RespArray.TYPE_PREFIX:
        return new RespArray(encodedBytes);
      case RespBulkString.TYPE_PREFIX:
        return new RespBulkString(encodedBytes);
      case RespError.TYPE_PREFIX:
        return new RespError(encodedBytes);
      case RespInteger.TYPE_PREFIX:
        return new RespInteger(encodedBytes);
      case RespSimpleString.TYPE_PREFIX:
        return new RespSimpleString(encodedBytes);
      default:
        throw new IllegalArgumentException("Unknown RespType");
    }
  }
}

package dev.sbutler.bitflask.resp.messages;

/** Code indicating the type of {@link RespRequest} being sent to the Bitflask server. */
public enum RespRequestCode {
  PING(0),
  GET(1),
  SET(2),
  DELETE(3);

  private final int value;

  RespRequestCode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static RespRequestCode fromValue(int value) {
    return switch (value) {
      case 0 -> RespRequestCode.PING;
      case 1 -> RespRequestCode.GET;
      case 2 -> RespRequestCode.SET;
      case 3 -> RespRequestCode.DELETE;
      default -> throw new IllegalArgumentException(
          String.format("No RespRequestCode exists for value [%d].", value));
    };
  }
}

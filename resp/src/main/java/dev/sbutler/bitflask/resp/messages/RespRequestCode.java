package dev.sbutler.bitflask.resp.messages;

/** Code indicating the type of {@link RespRequest} being sent to the Bitflask server. */
public enum RespRequestCode {
  GET(0),
  SET(1),
  DELETE(2);

  private final int value;

  RespRequestCode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static RespRequestCode fromValue(int value) {
    return switch (value) {
      case 0 -> RespRequestCode.GET;
      case 1 -> RespRequestCode.SET;
      case 2 -> RespRequestCode.DELETE;
      default -> throw new IllegalArgumentException(
          String.format("No RespRequestCode exists for value [%d].", value));
    };
  }
}

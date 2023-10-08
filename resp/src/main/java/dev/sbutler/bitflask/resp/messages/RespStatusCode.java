package dev.sbutler.bitflask.resp.messages;

/** Status codes for RESP based responses from a Bitflask server. */
public enum RespStatusCode {
  SUCCESS(0),
  FAILURE(1),
  NOT_CURRENT_LEADER(2),
  NO_KNOWN_LEADER(3);

  private final int value;

  RespStatusCode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static RespStatusCode fromValue(int value) {
    return switch (value) {
      case 0 -> RespStatusCode.SUCCESS;
      case 1 -> RespStatusCode.FAILURE;
      case 2 -> RespStatusCode.NOT_CURRENT_LEADER;
      case 3 -> RespStatusCode.NO_KNOWN_LEADER;
      default -> throw new IllegalArgumentException(
          String.format("No RespStatusCode exists for value [%d].", value));
    };
  }
}

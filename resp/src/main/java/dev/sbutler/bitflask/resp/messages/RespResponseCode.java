package dev.sbutler.bitflask.resp.messages;

/** Code indicating the type of {@link RespResponse} received from the Bitflask server. */
public enum RespResponseCode {
  SUCCESS(0),
  FAILURE(1),
  NOT_CURRENT_LEADER(2),
  NO_KNOWN_LEADER(3);

  private final int value;

  RespResponseCode(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static RespResponseCode fromValue(int value) {
    return switch (value) {
      case 0 -> RespResponseCode.SUCCESS;
      case 1 -> RespResponseCode.FAILURE;
      case 2 -> RespResponseCode.NOT_CURRENT_LEADER;
      case 3 -> RespResponseCode.NO_KNOWN_LEADER;
      default -> throw new IllegalArgumentException(
          String.format("No RespResponseCode exists for value [%d].", value));
    };
  }
}

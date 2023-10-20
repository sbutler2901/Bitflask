package dev.sbutler.bitflask.resp.messages;

/**
 * Used to indicate an issue when converting between {@link
 * dev.sbutler.bitflask.resp.types.RespElement} and {@link RespRequest}.
 */
public class RespRequestConversionException extends RespMessageException {

  public RespRequestConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}

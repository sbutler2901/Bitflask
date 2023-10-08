package dev.sbutler.bitflask.resp.messages;

/**
 * Used to indicate an issue when converting between {@link
 * dev.sbutler.bitflask.resp.types.RespElement} and {@link RespResponse}.
 */
public class RespResponseConversionException extends RuntimeException {

  public RespResponseConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}

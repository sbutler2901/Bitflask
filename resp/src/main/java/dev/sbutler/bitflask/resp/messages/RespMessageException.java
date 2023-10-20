package dev.sbutler.bitflask.resp.messages;

/**
 * A generic exception for Resp message related issues.
 *
 * <p>Prefer creating more specific exceptions as subclasses.
 */
public class RespMessageException extends RuntimeException {
  public RespMessageException() {
    super();
  }

  public RespMessageException(String message) {
    super(message);
  }

  public RespMessageException(String message, Throwable cause) {
    super(message, cause);
  }

  public RespMessageException(Throwable cause) {
    super(cause);
  }
}

package dev.sbutler.bitflask.client.command_processing;

/** Used to indicate an issue when processing a command. */
public class ProcessingException extends RuntimeException {

  public ProcessingException(String message) {
    super(message);
  }

  public ProcessingException(String message, Throwable cause) {
    super(message, cause);
  }
}

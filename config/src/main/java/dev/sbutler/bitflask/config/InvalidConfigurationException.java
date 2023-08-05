package dev.sbutler.bitflask.config;

/** Used to indicate an invalid configuration was found during validation. */
public class InvalidConfigurationException extends RuntimeException {

  public InvalidConfigurationException(String string) {
    super(string);
  }
}

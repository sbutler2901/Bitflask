package dev.sbutler.bitflask.common.configuration.exceptions;

import com.beust.jcommander.ParameterException;

public class IllegalConfigurationException extends ParameterException {

  public IllegalConfigurationException(Throwable t) {
    super(t);
  }

  public IllegalConfigurationException(String string) {
    super(string);
  }

  public IllegalConfigurationException(String string, Throwable t) {
    super(string, t);
  }
}

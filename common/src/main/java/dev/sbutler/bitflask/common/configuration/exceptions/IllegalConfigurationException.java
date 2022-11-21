package dev.sbutler.bitflask.common.configuration.exceptions;

import com.beust.jcommander.ParameterException;

public class IllegalConfigurationException extends ParameterException {

  public IllegalConfigurationException(String string) {
    super(string);
  }
}

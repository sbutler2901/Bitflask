package dev.sbutler.bitflask.config;

import com.beust.jcommander.ParameterException;

public class IllegalConfigurationException extends ParameterException {

  public IllegalConfigurationException(String string) {
    super(string);
  }
}

package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;

public interface InputParser {

  /**
   * Retrieves the user's input as a list of Strings
   */
  ImmutableList<String> getClientNextInput();

}

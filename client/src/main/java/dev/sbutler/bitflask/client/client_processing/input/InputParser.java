package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.text.ParseException;

public class InputParser {

  /**
   * Retrieves the user's input as a list of Strings.
   *
   * <p>{@code null} will be returned if the end of input has been reached
   */
  ImmutableList<String> getClientNextInput() throws IOException, ParseException {
    return ImmutableList.of();
  }

}

package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;

public class StdinInputParser implements InputParser {

  private final BufferedReader reader;

  public StdinInputParser() {
    this.reader = new BufferedReader(new InputStreamReader(System.in));
  }

  public ImmutableList<String> getClientNextInput() throws IOException, ParseException {
    String line = reader.readLine();
    if (line == null) {
      return null;
    }
    InputToArgsConverter argsConverter = new InputToArgsConverter(line);
    return argsConverter.convert();
  }
}

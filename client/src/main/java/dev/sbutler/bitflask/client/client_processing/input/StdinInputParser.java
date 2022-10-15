package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.util.Scanner;

public class StdinInputParser implements InputParser {


  private final Scanner inputScanner;

  public StdinInputParser() {
    this.inputScanner = new Scanner(System.in);
  }

  public ImmutableList<String> getClientNextInput() throws ParseException {
    String line = inputScanner.nextLine();
    InputToArgsConverter argsConverter = new InputToArgsConverter();
    return argsConverter.convert(line);
  }
}

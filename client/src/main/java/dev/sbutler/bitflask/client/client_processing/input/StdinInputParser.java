package dev.sbutler.bitflask.client.client_processing.input;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Scanner;

public class StdinInputParser implements InputParser {

  private static final String SPACE_REGEX = "\\s+";

  private final Scanner inputScanner;

  public StdinInputParser() {
    this.inputScanner = new Scanner(System.in);
  }

  public ImmutableList<String> getClientNextInput() {
    String line = inputScanner.nextLine();
    return Arrays.stream(line.split(SPACE_REGEX))
        .filter((arg) -> !arg.isBlank()).collect(toImmutableList());
  }

}

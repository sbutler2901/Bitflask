package dev.sbutler.bitflask.client.client_processing.input;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class StdinInputParser implements InputParser {

  private static final String SPACE_REGEX = "\\s+";

  private final Scanner inputScanner;

  public StdinInputParser() {
    this.inputScanner = new Scanner(System.in);
  }

  /**
   * Retrieves the next command submitted by the user and any arguments
   *
   * @return the command along with the args
   */
  public ClientCommand getNextCommand() {
    String line = inputScanner.nextLine();
    List<String> parsedInput = Arrays.stream(line.split(SPACE_REGEX))
        .filter((arg) -> !arg.isBlank()).collect(Collectors.toList());

    if (parsedInput.size() > 0) {
      String command = parsedInput.get(0);
      List<String> args = parsedInput.subList(1, parsedInput.size());
      return new ClientCommand(command, args);
    }
    return null;
  }

}

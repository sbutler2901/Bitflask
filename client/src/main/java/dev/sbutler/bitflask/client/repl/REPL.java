package dev.sbutler.bitflask.client.repl;

import dev.sbutler.bitflask.client.Client;
import dev.sbutler.bitflask.client.ClientCommand;
import dev.sbutler.bitflask.client.ReplCommand;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class REPL {

  private static final String SPACE_REGEX = "\\s+";

  private final Client client;
  private final Scanner input;

  private boolean continueReadingClientInput = true;

  /**
   * Creating a new REPL instance for accepting user command to interact with the storage engine
   *
   * @param client client
   */
  public REPL(Client client) {
    this.client = client;
    this.input = new Scanner(System.in);
  }

  /**
   * Retrieves the next command submitted by the user and any arguments
   *
   * @return the command along with the args
   */
  private ClientCommand getNextCommand() {
    String line = input.nextLine();
    List<String> parsedInput = Arrays.stream(line.split(SPACE_REGEX)).map(
        String::trim).collect(Collectors.toList());

    if (parsedInput.size() > 0) {
      String command = parsedInput.get(0);
      List<String> args = parsedInput.subList(1, parsedInput.size());
      return new ClientCommand(command, args);
    }
    return null;
  }

  /**
   * Runs the REPL loop
   */
  public void start() throws IOException {
    while (continueReadingClientInput) {
      System.out.print(client.getServerAddress() + "> ");
      ClientCommand clientCommand = getNextCommand();
      if (clientCommand == null) {
        continue;
      }

      if (ReplCommand.isReplCommand(clientCommand.command())) {
        processReplCommand(clientCommand);
      } else {
        String result = client.runCommand(clientCommand);
        System.out.println(result);
      }
    }
  }

  private void processReplCommand(ClientCommand clientCommand) throws IOException {
    ReplCommand replCommand = ReplCommand
        .valueOf(clientCommand.command().trim().toUpperCase());
    switch (replCommand) {
      case EXIT -> continueReadingClientInput = false;
      case TEST -> test(clientCommand);
      case HELP -> System.out.println("I can't help you.");
    }
  }

  /**
   * Initiates a test generating an arg provided number of set and optional get commands
   *
   * @param clientCommand the command with its required arguments
   */
  private void test(ClientCommand clientCommand) throws IOException {
    int numEntriesGenerated = Integer.parseInt(clientCommand.args().get(0));

    boolean withGet =
        clientCommand.args().size() > 1 && Boolean.parseBoolean(clientCommand.args().get(1));
    System.out
        .println("Generating " + numEntriesGenerated + " entries with get (" + withGet + ")");

    String result;
    long startTime = System.currentTimeMillis();
    int i;
    for (i = 0; i < numEntriesGenerated; i++) {
      String iteration = String.valueOf(i);
      String setKey = "testKey" + iteration;
      String value = "testValue" + iteration;

      result = client.runCommand(new ClientCommand("SET", Arrays.asList(setKey, value)));
      System.out.println("Result: " + result);
      if (withGet && i > 0) {
        int previousWrite = i - 1;
        String getKey = "testKey" + previousWrite;
        result = client.runCommand(new ClientCommand("GET", Collections.singletonList(getKey)));
        System.out.println("Result: " + result);
      }
    }
    if (withGet) {
      String lastTestKey = "testKey" + (i - 1);
      result = client.runCommand(new ClientCommand("GET", Collections.singletonList(lastTestKey)));
      System.out.println("Result: " + result);
    }

    long endTime = System.currentTimeMillis();
    long duration = (endTime - startTime);
    System.out
        .printf("(%d) sets successfully generated in (%d)ms%n", numEntriesGenerated, duration);
  }
}

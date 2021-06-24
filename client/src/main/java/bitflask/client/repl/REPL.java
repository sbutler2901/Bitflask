package bitflask.client.repl;

import bitflask.client.Client;
import bitflask.utilities.Command;
import bitflask.utilities.Commands;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class REPL {

  private static final String SPACE_REGEX = "\\s+";

  private static final String SET_LOG = "Saved (%s) with value (%s)";
  private static final String GET_LOG = "Read (%s) with value (%s)";

  private final Scanner input;
  private final Client client;

  private boolean loggingEnabled = true;

  /**
   * Creating a new REPL instance for accepting user command to interact with the storage engine
   *
   * @param client client
   */
  public REPL(Client client) {
    this.input = new Scanner(System.in);
    this.client = client;
  }

  /**
   * Retrieves the next command submitted by the user and any arguments
   *
   * @return the command along with the args
   */
  private Command getNextCommand() {
    String line = input.nextLine();
    String[] parsedInput = line.split(SPACE_REGEX);

    Commands command = Commands.INVALID;
    List<String> args = new ArrayList<>();
    for (int i = 0; i < parsedInput.length; i++) {
      if (i == 0) {
        command = Commands.from(parsedInput[i]);
      } else {
        args.add(parsedInput[i]);
      }
    }

    return new Command(command, args);
  }

  /**
   * Runs the REPL loop
   */
  public void start() {
    while (true) {
      Command command = getNextCommand();

      if (command.getCommand() == Commands.EXIT) {
        break;
      } else if (command.getCommand() == Commands.GET && command.getArgs().size() > 0) {
        client.sendCommand(command.getCommandRespArray());
      } else if (command.getCommand() == Commands.SET && command.getArgs().size() > 1) {
        client.sendCommand(command.getCommandRespArray());
      } else if (command.getCommand() == Commands.LOG && command.getArgs().size() > 0) {
        logging(command);
      } else if (command.getCommand() == Commands.TEST && command.getArgs().size() > 0) {
        test(command);
      } else {
        System.out.println("Invalid input!");
      }
    }
  }

  /**
   * Gets the value of a key from persistent storage, if it exists
   *
   * @param Command the get command with its required arguments
   */
  private void get(Command Command) {
    String key = Command.getArgs().get(0);

//    Optional<String> optionalValue = storage.read(key);
    client.sendCommand(Command.getCommandRespArray());
    Optional<String> optionalValue = Optional.empty();

    String readValue = optionalValue.orElse("Key not found");
    if (loggingEnabled) {
      System.out.printf((GET_LOG) + "%n", key, readValue);
    }
  }

  /**
   * Sets stores a corresponding key and value in persistent storage
   *
   * @param Command the set command with its required arguments
   */
  private void set(Command Command) {
    String key = Command.getArgs().get(0);
    String value = Command.getArgs().get(1);

    //      storage.write(key, value);
    if (loggingEnabled) {
      System.out.printf((SET_LOG) + "%n", key, value);
    }
  }

  /**
   * Enables logging for the get and set commands
   *
   * @param Command the log command with its required arguments
   */
  private void logging(Command Command) {
    loggingEnabled = Command.getArgs().get(0).equals("true");
    if (loggingEnabled) {
      System.out.println("Logging has been enabled");
    } else {
      System.out.println("Logging has been disabled");
    }
  }

  /**
   * Initiates a test generating an arg provided number of set and optional get commands
   *
   * @param Command the command with its required arguments
   */
  private void test(Command Command) {
    // TODO: move to server side?
    int numEntriesGenerated = Integer.parseInt(Command.getArgs().get(0));

    boolean withGet =
        Command.getArgs().size() > 2 && Boolean.parseBoolean(Command.getArgs().get(1));
    System.out
        .println("Generating " + numEntriesGenerated + " entries with get (" + withGet + ")");

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numEntriesGenerated; i++) {
      String iteration = String.valueOf(i);
      String setKey = "testKey" + iteration;
      String value = "testValue" + iteration;
      set(new Command(Commands.SET, Arrays.asList(setKey, value)));
      if (withGet && i > 0) {
        int previousWrite = i - 1;
        String getKey = "testKey" + previousWrite;
        get(new Command(Commands.GET, Collections.singletonList(getKey)));
      }
    }
    if (withGet) {
      String lastTestKey = "testKey" + numEntriesGenerated;
      set(new Command(Commands.GET, Collections.singletonList(lastTestKey)));
    }

    long endTime = System.currentTimeMillis();
    long duration = (endTime - startTime);
    System.out
        .printf("(%d) sets successfully generated in (%d)ms%n", numEntriesGenerated, duration);
  }
}

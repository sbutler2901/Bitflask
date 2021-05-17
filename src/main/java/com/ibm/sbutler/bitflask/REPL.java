package com.ibm.sbutler.bitflask;

import com.ibm.sbutler.bitflask.storage.Storage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * The various commands recognized by the REPL
 */
enum Command {
  EXIT,
  GET,
  SET,
  LOG,
  TEST,
  INVALID
}

/**
 * A simple wrapper for a REPL command and its args
 */
final class CommandWithArgs {

  Command command;
  List<String> args;

  CommandWithArgs(Command command, List<String> args) {
    this.command = command;
    this.args = args;
  }
}

class REPL implements Runnable {

  private static final String SET_LOG = "Saved (%s) with value (%s)";
  private static final String GET_LOG = "Read (%s) with value (%s)";

  private final Scanner input;
  private final Storage storage;

  private boolean loggingEnabled = true;

  /**
   * Creating a new REPL instance for accepting user command to interact with the storage engine
   *
   * @param storage the storage instance for getting and setting key-value pairs
   */
  REPL(Storage storage) {
    this.input = new Scanner(System.in);
    this.storage = storage;
  }

  /**
   * Parses the user provided command
   *
   * @param commandString the command provided by the user
   * @return the corresponding command
   */
  private Command parseCommand(String commandString) {
    switch (commandString) {
      case "exit":
        return Command.EXIT;
      case "get":
        return Command.GET;
      case "set":
        return Command.SET;
      case "log":
        return Command.LOG;
      case "test":
        return Command.TEST;
      default:
        return Command.INVALID;
    }
  }

  /**
   * Retrieves the next command submitted by the user and any arguments
   *
   * @return the command along with the args
   */
  private CommandWithArgs getNextCommand() {
    String line = input.nextLine();
    String[] parsedInput = line.split("\\s+");

    Command command = Command.INVALID;
    List<String> args = new ArrayList<>();
    for (int i = 0; i < parsedInput.length; i++) {
      if (i == 0) {
        command = parseCommand(parsedInput[i]);
      } else {
        args.add(parsedInput[i]);
      }
    }

    return new CommandWithArgs(command, args);
  }

  /**
   * Runs the REPL loop
   */
  private void start() {
    while (true) {
      CommandWithArgs commandWithArgs = getNextCommand();
      if (commandWithArgs.command == Command.EXIT) {
        break;
      } else if (commandWithArgs.command == Command.GET && commandWithArgs.args.size() > 0) {
        get(commandWithArgs);
      } else if (commandWithArgs.command == Command.SET && commandWithArgs.args.size() > 1) {
        set(commandWithArgs);
      } else if (commandWithArgs.command == Command.LOG && commandWithArgs.args.size() > 0) {
        logging(commandWithArgs);
      } else if (commandWithArgs.command == Command.TEST && commandWithArgs.args.size() > 0) {
        test(commandWithArgs);
      } else {
        System.out.println("Invalid input!");
      }
    }
  }

  /**
   * Gets the value of a key from persistent storage, if it exists
   *
   * @param commandWithArgs the get command with its required arguments
   */
  private void get(CommandWithArgs commandWithArgs) {
    String key = commandWithArgs.args.get(0);

    Optional<String> optionalValue = storage.read(key);
    String readValue = optionalValue.orElse("Key not found");
    if (loggingEnabled) {
      System.out.printf((GET_LOG) + "%n", key, readValue);
    }
  }

  /**
   * Sets stores a corresponding key and value in persistent storage
   *
   * @param commandWithArgs the set command with its required arguments
   */
  private void set(CommandWithArgs commandWithArgs) {
    String key = commandWithArgs.args.get(0);
    String value = commandWithArgs.args.get(1);

    try {
      storage.write(key, value);
      if (loggingEnabled) {
        System.out.printf((SET_LOG) + "%n", key, value);
      }
    } catch (IOException e) {
      System.out.println("There was an issue saving the key and value to storage");
      e.printStackTrace();
    }
  }

  /**
   * Enables logging for the get and set commands
   *
   * @param commandWithArgs the log command with its required arguments
   */
  private void logging(CommandWithArgs commandWithArgs) {
    loggingEnabled = commandWithArgs.args.get(0).equals("true");
    if (loggingEnabled) {
      System.out.println("Logging has been enabled");
    } else {
      System.out.println("Logging has been disabled");
    }
  }

  /**
   * Initiates a test generating an arg provided number of set and optional get commands
   *
   * @param commandWithArgs the command with its required arguments
   */
  private void test(CommandWithArgs commandWithArgs) {
    int numEntriesGenerated = new Integer(commandWithArgs.args.get(0));

    boolean withGet =
        commandWithArgs.args.size() > 2 && Boolean.parseBoolean(commandWithArgs.args.get(1));
    System.out
        .println("Generating " + numEntriesGenerated + " entries with get (" + withGet + ")");

    long startTime = System.currentTimeMillis();
    for (int i = 0; i < numEntriesGenerated; i++) {
      String iteration = String.valueOf(i);
      String setKey = "testKey" + iteration;
      String value = "testValue" + iteration;
      set(new CommandWithArgs(Command.SET, Arrays.asList(setKey, value)));
      if (withGet && i > 0) {
        int previousWrite = i - 1;
        String getKey = "testKey" + previousWrite;
        get(new CommandWithArgs(Command.GET, Collections.singletonList(getKey)));
      }
    }
    if (withGet) {
      String lastTestKey = "testKey" + numEntriesGenerated;
      set(new CommandWithArgs(Command.GET, Collections.singletonList(lastTestKey)));
    }

    long endTime = System.currentTimeMillis();
    long duration = (endTime - startTime);
    System.out
        .printf("(%d) sets successfully generated in (%d)ms%n", numEntriesGenerated, duration);
  }

  @Override
  public void run() {
    start();
  }
}

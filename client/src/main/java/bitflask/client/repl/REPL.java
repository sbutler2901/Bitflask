package bitflask.client.repl;

import bitflask.resp.Resp;
import bitflask.resp.RespType;
import bitflask.utilities.Command;
import bitflask.utilities.Commands;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class REPL {

  private static final String SPACE_REGEX = "\\s+";

  private static final String SET_LOG = "Saved (%s) with value (%s)";
  private static final String GET_LOG = "Read (%s) with value (%s)";

  private final Scanner input;
  private final Resp resp;

  private boolean loggingEnabled = true;

  /**
   * Creating a new REPL instance for accepting user command to interact with the storage engine
   *
   * @param resp resp
   */
  public REPL(Resp resp) {
    this.input = new Scanner(System.in);
    this.resp = resp;
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
      try {
        Command command = getNextCommand();

        if (command.getCommand() == Commands.EXIT) {
          break;
        } else if (command.getCommand() == Commands.GET && command.getArgs().size() > 0) {
          resp.send(command.getCommandRespArray());
          RespType respType = resp.receive();
          System.out.println(respType);
        } else if (command.getCommand() == Commands.SET && command.getArgs().size() > 1) {
          resp.send(command.getCommandRespArray());
          RespType respType = resp.receive();
          System.out.println(respType);
        } else if (command.getCommand() == Commands.LOG && command.getArgs().size() > 0) {
          logging(command);
        } else if (command.getCommand() == Commands.TEST && command.getArgs().size() > 0) {
          test(command);
        } else {
          System.out.println("Invalid input!");
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
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
  private void test(Command Command) throws IOException {
    int numEntriesGenerated = Integer.parseInt(Command.getArgs().get(0));

    boolean withGet =
        Command.getArgs().size() > 1 && Boolean.parseBoolean(Command.getArgs().get(1));
    System.out
        .println("Generating " + numEntriesGenerated + " entries with get (" + withGet + ")");

    RespType response;
    long startTime = System.currentTimeMillis();
    int i;
    for (i = 0; i < numEntriesGenerated; i++) {
      String iteration = String.valueOf(i);
      String setKey = "testKey" + iteration;
      String value = "testValue" + iteration;

      resp.send(new Command(Commands.SET, Arrays.asList(setKey, value)).getCommandRespArray());
      response = resp.receive();
      System.out.println("Response: " + response);
      if (withGet && i > 0) {
        int previousWrite = i - 1;
        String getKey = "testKey" + previousWrite;
        resp.send(new Command(Commands.GET, Collections.singletonList(getKey)).getCommandRespArray());
        response = resp.receive();
        System.out.println("Response: " + response);
      }
    }
    if (withGet) {
      String lastTestKey = "testKey" + (i - 1);
      resp.send(new Command(Commands.GET, Collections.singletonList(lastTestKey)).getCommandRespArray());
      response = resp.receive();
      System.out.println("Response: " + response);
    }

    long endTime = System.currentTimeMillis();
    long duration = (endTime - startTime);
    System.out
        .printf("(%d) sets successfully generated in (%d)ms%n", numEntriesGenerated, duration);
  }
}

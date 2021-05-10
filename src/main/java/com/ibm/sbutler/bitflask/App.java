package com.ibm.sbutler.bitflask;

import com.ibm.sbutler.bitflask.storage.Storage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Scanner;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class App {

  private static final String SET_LOG = "Saved (%s) with value (%s)";
  private static final String GET_LOG = "Read (%s) with value (%s)";

  private final Storage storage;

  private boolean loggingEnabled = false;

  App() throws FileNotFoundException {
    this(new Storage());
  }

  App(Storage storage) {
    this.storage = storage;
  }

  /**
   * Sets stores a corresponding key and value in persistent storage
   *
   * @param key   the key to use for accessing the data
   * @param value the data to be persisted
   */
  private void set(String key, String value) {
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
   * Gets the value of a key from persistent storage, if it exists
   *
   * @param key the key used for retrieving persisted data
   * @return the persisted data, or null if key has not been persisted
   */
   private String get(String key) {
    String readValue = "";
    try {
      Optional<String> optionalValue = storage.read(key);
      readValue = optionalValue.orElse("Key not found");
      if (loggingEnabled) {
        System.out.printf((GET_LOG) + "%n", key, readValue);
      }
    } catch (IOException e) {
      System.out.println("There was an issue getting the provided key's value from storage");
      e.printStackTrace();
    }
    return readValue;
  }

  private void start() {
    System.out.println("Welcome to Bitflask!");
    Scanner input = new Scanner(System.in);

    while (true) {
      String line = input.nextLine();
      String[] parsedInput = line.split("\\s+");

      String command = parsedInput[0];

      if (command.startsWith("!exit")) {
        break;
      } else if (command.startsWith("get") && parsedInput.length > 1) {
        String key = parsedInput[1];

        String retrievedValue = get(key);
        System.out.println("> " + retrievedValue);
      } else if (command.startsWith("set") && parsedInput.length > 2) {
        String key = parsedInput[1];
        String value = parsedInput[2];

        set(key, value);
      } else if (command.startsWith("log") && parsedInput.length > 1) {
        loggingEnabled = parsedInput[1].equals("true");
      } else if (command.startsWith("test") && parsedInput.length > 1) {
        int generatedSets = new Integer(parsedInput[1]);
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < generatedSets; i++) {
          String iteration = String.valueOf(i);
          String key = "testKey" + iteration;
          String value = "testValue" + iteration;
          set(key, value);
        }
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime);
        System.out
            .printf("(%d) sets successfully generated in (%d)ms%n", generatedSets, duration);
      } else {
        System.out.println("Invalid input!");
      }
    }
  }

  public static void main(String[] args) {
    try {
      App app = new App();
      app.start();
      System.exit(0);
    } catch (IOException e) {
      System.out.println("Unable to initialize storage engine. Terminating");
      e.printStackTrace();
      System.exit(1);
    }
  }
}

package com.ibm.sbutler.bitflask;

import com.ibm.sbutler.bitflask.storage.Storage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class App {

  private static final String SET_LOG = "Saved (%s) with value (%s)";
  private static final String GET_LOG = "Read (%s) with value (%s)";

  private final Storage storage;

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
  public void set(String key, String value) {
    try {
      storage.write(key, value);
      System.out.printf((SET_LOG) + "%n", key, value);
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
  public String get(String key) {
    String readValue = "";
    try {
      Optional<String> optionalValue = storage.read(key);
      readValue = optionalValue.orElse("");
      System.out.printf((GET_LOG) + "%n", key, readValue);
    } catch (IOException e) {
      System.out.println("There was an issue getting the provided key's value from storage");
      e.printStackTrace();
    }
    return readValue;
  }
}

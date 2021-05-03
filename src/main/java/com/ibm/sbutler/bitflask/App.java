package com.ibm.sbutler.bitflask;

import com.ibm.sbutler.bitflask.storage.Storage;
import com.ibm.sbutler.bitflask.storage.StorageEntry;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides support for getting and setting key value pairs with persistence
 */
public class App {
  private static final String SET_LOG = "Saved (%s) with value (%s): %s";
  private static final String GET_LOG = "Read (%s) with value (%s): %s";
  private static final String GET_ERR_KEY_NOT_FOUND = "Error reading (%s), offset entry not found";

  private final Storage storage;
  private final Map<String, StorageEntry> offsetMap = new HashMap<>();

  App() throws FileNotFoundException {
    this(new Storage());
  }

  App(Storage storage) {
    this.storage = storage;
  }

  /**
   * Sets stores a corresponding key and value in persistent storage
   * @param key the key to use for accessing the data
   * @param value the data to be persisted
   * @throws IOException when an error occurs persisting the data
   */
  public void set(String key, String value) throws IOException {
    StorageEntry storageEntry = storage.write(value.getBytes(StandardCharsets.UTF_8));
    offsetMap.put(key, storageEntry);

    System.out.printf((SET_LOG) + "%n", key, value, storageEntry);
  }

  /**
   * Gets the value of a key from persistent storage, if it exists
   * @param key the key used for retrieving persisted data
   * @return the persisted data, or null if key has not been persisted
   * @throws IOException when an error occurs retrieving the data
   */
  public String get(String key) throws IOException {
    StorageEntry storageEntry = offsetMap.get(key);

    if (storageEntry == null) {
      System.out.printf((GET_ERR_KEY_NOT_FOUND) + "%n", key);
      return null;
    }

    byte[] readBytes = storage.read(storageEntry);
    String value = new String(readBytes);

    System.out.printf((GET_LOG) + "%n", key, value, storageEntry);

    return value;
  }
}

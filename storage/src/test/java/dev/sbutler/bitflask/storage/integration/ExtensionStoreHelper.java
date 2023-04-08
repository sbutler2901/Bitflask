package dev.sbutler.bitflask.storage.integration;

import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Provides helper methods for interacting with an {@link ExtensionContext.Store}.
 */
public class ExtensionStoreHelper {

  private final ExtensionContext.Store store;

  public ExtensionStoreHelper(ExtensionContext.Store store) {
    this.store = store;
  }

  /**
   * Puts the object in the store deriving the key from the Object's class name
   */
  private void putInStore(Object object) {
    putInStore(object.getClass().getName(), object);
  }

  /**
   * Puts the object in the store with the provided key.
   */
  private void putInStore(String key, Object object) {
    store.put(key, object);
  }

  /**
   * Gets an Object from the store keyed by the provided class's name.
   */
  private <T> T getFromStore(Class<T> clazz) {
    return getFromStore(clazz.getName(), clazz);
  }

  /**
   * Gets an object from the store with the provided key.
   */
  private <T> T getFromStore(String key, Class<T> clazz) {
    return store.get(key, clazz);
  }
}

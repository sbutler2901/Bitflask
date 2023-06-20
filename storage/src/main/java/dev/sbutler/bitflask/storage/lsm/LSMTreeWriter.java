package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageWriteException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import jakarta.inject.Inject;
import java.io.IOException;

/**
 * Handles write related tasks for the {@link LSMTree}.
 */
final class LSMTreeWriter {

  private final LSMTreeStateManager stateManager;

  @Inject
  LSMTreeWriter(LSMTreeStateManager stateManager) {
    this.stateManager = stateManager;
  }

  /**
   * Writes the {@link dev.sbutler.bitflask.storage.lsm.entry.Entry}.
   */
  void write(Entry entry) {
    try (var currentState = stateManager.getCurrentState()) {
      try {
        currentState.getMemtable().write(entry);
      } catch (IOException e) {
        throw new StorageWriteException(e);
      }
    }
  }
}

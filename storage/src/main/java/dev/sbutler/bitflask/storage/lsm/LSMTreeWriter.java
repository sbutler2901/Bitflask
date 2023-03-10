package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageWriteException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Handles write related tasks for the {@link LSMTree}.
 */
final class LSMTreeWriter {

  private final LSMTreeStateManager stateManager;

  @Inject
  LSMTreeWriter(LSMTreeStateManager stateManager) {
    this.stateManager = stateManager;
  }

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

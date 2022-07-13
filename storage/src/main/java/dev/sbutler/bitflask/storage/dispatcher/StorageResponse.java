package dev.sbutler.bitflask.storage.dispatcher;

import java.util.Optional;

public record StorageResponse(Status status, Optional<String> response,
                              Optional<String> errorMessage) {

  public enum Status {
    OK,
    FAILED
  }
}

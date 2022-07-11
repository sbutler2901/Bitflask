package dev.sbutler.bitflask.storage;

import java.util.Optional;

public record StorageResponse(Status status, Optional<String> response,
                              Optional<String> errorMessage) {

  public enum Status {
    OK,
    FAILED
  }
}

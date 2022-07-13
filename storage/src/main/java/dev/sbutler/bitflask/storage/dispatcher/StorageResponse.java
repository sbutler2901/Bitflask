package dev.sbutler.bitflask.storage.dispatcher;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

public record StorageResponse(Status status, Optional<String> response,
                              Optional<String> errorMessage) {

  public enum Status {
    OK,
    FAILED
  }

  public StorageResponse {
    if (status.equals(Status.OK)) {
      checkArgument(response.isPresent(), "response should be present when status is OK");
      checkArgument(errorMessage.isEmpty(), "errorMessage should be empty when status is FAILED");
    } else {
      checkArgument(errorMessage.isPresent(),
          "errorMessage should be present when status is FAILED");
      checkArgument(response.isEmpty(), "response should be empty when status is FAILED");
    }
  }
}

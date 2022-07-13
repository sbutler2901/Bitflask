package dev.sbutler.bitflask.storage.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Status;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class StorageResponseTest {

  @Test
  public void construction() {
    StorageResponse ok = new StorageResponse(Status.OK, Optional.of("ok"), Optional.empty());
    assertEquals("ok", ok.response().get());
    StorageResponse failed = new StorageResponse(Status.FAILED, Optional.empty(),
        Optional.of("failed"));
    assertEquals("failed", failed.errorMessage().get());
  }

  @Test
  public void validateOk() {
    assertThrows(IllegalArgumentException.class,
        () -> new StorageResponse(Status.OK, Optional.empty(), Optional.empty()));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageResponse(Status.OK, Optional.of("ok"), Optional.of("failed")));
  }

  @Test
  public void validateFailed() {
    assertThrows(IllegalArgumentException.class,
        () -> new StorageResponse(Status.FAILED, Optional.empty(), Optional.empty()));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageResponse(Status.FAILED, Optional.of("ok"), Optional.of("failed")));
  }
}

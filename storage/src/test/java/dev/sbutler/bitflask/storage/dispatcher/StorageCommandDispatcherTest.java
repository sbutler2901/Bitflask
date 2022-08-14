package dev.sbutler.bitflask.storage.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

public class StorageCommandDispatcherTest {

  @Test
  public void capacity() throws Exception {
    // Arrange
    StorageCommandDispatcher storageCommandDispatcher = new StorageCommandDispatcher(1);
    storageCommandDispatcher.offer(new ReadDTO("key"));
    // Act
    ListenableFuture<StorageResponse> responseFuture =
        storageCommandDispatcher.offer(new ReadDTO("key"));
    // Assert
    assertTrue(responseFuture.isDone());
    try {
      responseFuture.get();
    } catch (ExecutionException e) {
      assertEquals(IllegalStateException.class, e.getCause().getClass());
    }
  }
}

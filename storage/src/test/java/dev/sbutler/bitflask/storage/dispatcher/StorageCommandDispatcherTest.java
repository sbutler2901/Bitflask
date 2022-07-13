package dev.sbutler.bitflask.storage.dispatcher;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommand.Type;
import org.junit.jupiter.api.Test;

public class StorageCommandDispatcherTest {

  @Test
  public void capacity() {
    StorageCommandDispatcher storageCommandDispatcher = new StorageCommandDispatcher(0);
    assertThrows(IllegalStateException.class,
        () -> storageCommandDispatcher.offer(new StorageCommand(
            Type.READ, ImmutableList.of("key"))));
  }
}

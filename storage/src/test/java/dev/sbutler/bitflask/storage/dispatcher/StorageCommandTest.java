package dev.sbutler.bitflask.storage.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommand.Type;
import org.junit.jupiter.api.Test;

public class StorageCommandTest {

  @Test
  public void construction() {
    StorageCommand storageCommand = new StorageCommand(Type.READ, ImmutableList.of("key"));
    assertEquals(Type.READ, storageCommand.type());
    assertEquals(ImmutableList.of("key"), storageCommand.arguments());
  }

  @Test
  public void validateReadArgs() {
    // arg size
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.READ, ImmutableList.of()));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.READ, ImmutableList.of("key", "value")));
    // Key validation
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.READ, ImmutableList.of("")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.READ, ImmutableList.of("    ")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.READ, ImmutableList.of(new String(new byte[257]))));
  }

  @Test
  public void validateWriteArgs() {
    // arg size
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of()));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("key")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("key", "value0", "value1")));
    // Key validation
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("", "value")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("    ", "value")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of(new String(new byte[257]), "value")));
    // Value validation
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("key", "")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("key", "    ")));
    assertThrows(IllegalArgumentException.class,
        () -> new StorageCommand(Type.WRITE, ImmutableList.of("key", new String(new byte[257]))));
  }
}

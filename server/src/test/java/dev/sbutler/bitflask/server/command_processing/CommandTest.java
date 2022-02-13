package dev.sbutler.bitflask.server.command_processing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CommandTest {

  @Test
  void isValidCommandArgs_null() {
    assertFalse(Command.isValidCommandArgs(null, null));
  }

  @Test
  void isValidCommandArgs_get() {
    assertTrue(Command.isValidCommandArgs(Command.GET, List.of("test")));
    assertFalse(Command.isValidCommandArgs(Command.GET, null));
    assertFalse(Command.isValidCommandArgs(Command.GET, List.of()));
    assertFalse(Command.isValidCommandArgs(Command.GET, List.of("test0", "test1")));
  }

  @Test
  void isValidCommandArgs_set() {
    assertTrue(Command.isValidCommandArgs(Command.SET, List.of("test0", "test1")));
    assertFalse(Command.isValidCommandArgs(Command.SET, null));
    assertFalse(Command.isValidCommandArgs(Command.SET, List.of()));
    assertFalse(Command.isValidCommandArgs(Command.SET, List.of("test0")));
    assertFalse(Command.isValidCommandArgs(Command.SET, List.of("test0", "test1", "test2")));
  }

  @Test
  void isValidCommandArgs_ping() {
    assertTrue(Command.isValidCommandArgs(Command.PING, null));
    assertTrue(Command.isValidCommandArgs(Command.PING, List.of()));
    assertFalse(Command.isValidCommandArgs(Command.PING, List.of("test")));
  }

}

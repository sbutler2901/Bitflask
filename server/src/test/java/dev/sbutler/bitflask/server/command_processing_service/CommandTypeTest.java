package dev.sbutler.bitflask.server.command_processing_service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class CommandTypeTest {

  @Test
  void isValidCommandArgs_null() {
    assertFalse(CommandType.isValidCommandArgs(null, null));
  }

  @Test
  void isValidCommandArgs_get() {
    assertTrue(CommandType.isValidCommandArgs(CommandType.GET, List.of("test")));
    assertFalse(CommandType.isValidCommandArgs(CommandType.GET, null));
    assertFalse(CommandType.isValidCommandArgs(CommandType.GET, List.of()));
    assertFalse(CommandType.isValidCommandArgs(CommandType.GET, List.of("test0", "test1")));
  }

  @Test
  void isValidCommandArgs_set() {
    assertTrue(CommandType.isValidCommandArgs(CommandType.SET, List.of("test0", "test1")));
    assertFalse(CommandType.isValidCommandArgs(CommandType.SET, null));
    assertFalse(CommandType.isValidCommandArgs(CommandType.SET, List.of()));
    assertFalse(CommandType.isValidCommandArgs(CommandType.SET, List.of("test0")));
    assertFalse(
        CommandType.isValidCommandArgs(CommandType.SET, List.of("test0", "test1", "test2")));
  }

  @Test
  void isValidCommandArgs_ping() {
    assertTrue(CommandType.isValidCommandArgs(CommandType.PING, null));
    assertTrue(CommandType.isValidCommandArgs(CommandType.PING, List.of()));
    assertFalse(CommandType.isValidCommandArgs(CommandType.PING, List.of("test")));
  }

}

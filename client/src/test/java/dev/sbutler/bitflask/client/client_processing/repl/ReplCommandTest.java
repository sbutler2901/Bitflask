package dev.sbutler.bitflask.client.client_processing.repl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ReplCommandTest {

  @Test
  void isReplCommand() {
    assertTrue(ReplCommand.isReplCommand("EXIT"));
    assertTrue(ReplCommand.isReplCommand(" exit"));
    assertTrue(ReplCommand.isReplCommand("HELP"));
    assertTrue(ReplCommand.isReplCommand(" help "));

    assertFalse(ReplCommand.isReplCommand(null));
    assertFalse(ReplCommand.isReplCommand(""));
    assertFalse(ReplCommand.isReplCommand("exita"));
    assertFalse(ReplCommand.isReplCommand("hel"));
  }

  @Test
  void isValidReplCommandWithArgs() {
    // null
    // EXIT
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.EXIT, null));
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.EXIT, new ArrayList<>()));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.EXIT, List.of("test-arg")));

    // HELP
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.HELP, null));
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.HELP, new ArrayList<>()));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.HELP, List.of("test")));
  }

}

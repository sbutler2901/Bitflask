package dev.sbutler.bitflask.client.repl;

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
    assertTrue(ReplCommand.isReplCommand("TEST"));
    assertTrue(ReplCommand.isReplCommand("test "));
    assertTrue(ReplCommand.isReplCommand("HELP"));
    assertTrue(ReplCommand.isReplCommand(" help "));

    assertFalse(ReplCommand.isReplCommand(null));
    assertFalse(ReplCommand.isReplCommand(""));
    assertFalse(ReplCommand.isReplCommand("exita"));
    assertFalse(ReplCommand.isReplCommand("btest"));
    assertFalse(ReplCommand.isReplCommand("hel"));
  }

  @Test
  void isValidReplCommandWithArgs() {
    // null
    // EXIT
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.EXIT, null));
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.EXIT, new ArrayList<>()));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.EXIT, List.of("test")));

    // HELP
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.HELP, null));
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.HELP, new ArrayList<>()));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.HELP, List.of("test")));

    // TEST
    assertTrue(ReplCommand.isValidReplCommandWithArgs(ReplCommand.TEST, List.of("test")));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.TEST, null));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.TEST, new ArrayList<>()));
    assertFalse(ReplCommand.isValidReplCommandWithArgs(ReplCommand.TEST, List.of("test", "other")));
  }

}

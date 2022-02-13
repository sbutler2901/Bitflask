package dev.sbutler.bitflask.server.command_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sbutler.bitflask.resp.RespArray;
import dev.sbutler.bitflask.resp.RespBulkString;
import dev.sbutler.bitflask.resp.RespInteger;
import dev.sbutler.bitflask.resp.RespSimpleString;
import dev.sbutler.bitflask.resp.RespType;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServerCommandTest {

  @Test
  void constructor_valid() {
    ServerCommand command = new ServerCommand(Command.PING, null);
    assertEquals(Command.PING, command.command());
  }

  @Test
  void constructor_invalid() {
    assertThrows(IllegalArgumentException.class,
        () -> new ServerCommand(Command.PING, List.of("test")));
  }

  @Test
  void valueOf_command() {
    RespType<?> commandMessage = new RespArray(List.of(new RespBulkString("ping")));
    ServerCommand command = ServerCommand.valueOf(commandMessage);
    assertEquals(Command.PING, command.command());
  }

  @Test
  void valueOf_args() {
    String key = "test";
    RespType<?> commandMessage = new RespArray(
        List.of(
            new RespBulkString("get"),
            new RespBulkString(key)
        )
    );
    ServerCommand command = ServerCommand.valueOf(commandMessage);
    assertEquals(Command.GET, command.command());
    assertEquals(List.of(key), command.args());
  }

  @Test
  void valueOf_IllegalArgumentException_commandMessage() {
    assertThrows(IllegalArgumentException.class, () -> ServerCommand.valueOf(new RespInteger(1)));
  }

  @Test
  void valueOf_IllegalArgumentException_command_type() {
    RespType<?> commandMessage = new RespArray(List.of(new RespSimpleString("ping")));
    assertThrows(IllegalArgumentException.class, () -> ServerCommand.valueOf(commandMessage));
  }

  @Test
  void valueOf_IllegalArgumentException_command_unknown() {
    RespType<?> commandMessage = new RespArray(List.of(new RespBulkString("bad-command")));
    assertThrows(IllegalArgumentException.class, () -> ServerCommand.valueOf(commandMessage));
  }

  @Test
  void valueOf_IllegalArgumentException_args_type() {
    RespType<?> commandMessage = new RespArray(
        List.of(
            new RespBulkString("get"),
            new RespSimpleString("key")
        )
    );
    assertThrows(IllegalArgumentException.class, () -> ServerCommand.valueOf(commandMessage));
  }
}

package dev.sbutler.bitflask.server.command_processing_service.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespInteger;
import dev.sbutler.bitflask.resp.types.RespSimpleString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ServerCommandTest {

  @Test
  void valueOf_command() {
    RespType<?> commandMessage = new RespArray(List.of(new RespBulkString("ping")));
    ServerCommand command = ServerCommand.valueOf(commandMessage);
    assertInstanceOf(PingCommand.class, command);
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
    assertInstanceOf(GetCommand.class, command);
    assertEquals(key, ((GetCommand) command).getKey());
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

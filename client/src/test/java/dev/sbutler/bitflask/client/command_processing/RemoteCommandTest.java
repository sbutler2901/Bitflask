package dev.sbutler.bitflask.client.command_processing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RemoteCommandTest {

  @Test
  void getAsRespArray() {
    String command = "GET";
    ImmutableList<String> args = ImmutableList.of("test-key");
    RespArray expected = new RespArray(List.of(
        new RespBulkString(command),
        new RespBulkString(args.get(0))
    ));
    RemoteCommand clientCommand = new RemoteCommand(command, args);

    assertEquals(expected, clientCommand.getAsRespArray());
  }

  @Test
  void getAsRespArray_nullArgs() {
    String command = "GET";
    RespArray expected = new RespArray(ImmutableList.of(new RespBulkString(command)));
    RemoteCommand clientCommand = new RemoteCommand(command, ImmutableList.of());
    assertEquals(expected, clientCommand.getAsRespArray());
  }
}

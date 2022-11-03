package dev.sbutler.bitflask.client.command_processing;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;

public record RemoteCommand(String command, ImmutableList<String> args) implements ClientCommand {

  public RemoteCommand {
    checkNotNull(command, args);
  }

  public RespArray getAsRespArray() {
    ImmutableList.Builder<RespElement> arrayArgs = ImmutableList.builder();
    arrayArgs.add(new RespBulkString(command));

    if (args != null) {
      for (String arg : args) {
        arrayArgs.add(new RespBulkString(arg));
      }
    }

    return new RespArray(arrayArgs.build());
  }
}

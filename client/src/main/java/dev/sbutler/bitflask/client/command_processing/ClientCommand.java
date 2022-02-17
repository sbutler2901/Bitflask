package dev.sbutler.bitflask.client.command_processing;

import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespType;
import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;

public record ClientCommand(@NonNull String command, List<String> args) {

  public RespArray getAsRespArray() {
    List<RespType<?>> arrayArgs = new ArrayList<>();
    arrayArgs.add(new RespBulkString(this.command));

    if (args != null) {
      for (String arg : args) {
        arrayArgs.add(new RespBulkString(arg));
      }
    }

    return new RespArray(arrayArgs);
  }
}

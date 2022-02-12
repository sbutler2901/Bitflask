package dev.sbutler.bitflask.client;

import dev.sbutler.bitflask.resp.RespArray;
import dev.sbutler.bitflask.resp.RespBulkString;
import dev.sbutler.bitflask.resp.RespType;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NonNull;

public class ClientCommand {
  @Getter
  private final String command;
  @Getter
  private final List<String> args;

  public ClientCommand(@NonNull String command, @NonNull List<String> args) {
    this.command = command.trim();
    this.args = args;
  }

  public RespArray getCommandRespArray() {
    List<RespType<?>> arrayArgs = new ArrayList<>();
    arrayArgs.add(new RespBulkString(this.command));
    this.args.forEach(arg -> arrayArgs.add(new RespBulkString(arg)));
    return new RespArray(arrayArgs);
  }
}

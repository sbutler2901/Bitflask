package bitflask.utilities;

import dev.sbutler.bitflask.resp.RespArray;
import dev.sbutler.bitflask.resp.RespBulkString;
import dev.sbutler.bitflask.resp.RespType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

/**
 * A simple wrapper for a REPL command and its args
 */
public class Command {

  @Getter
  private final Commands command;
  @Getter
  private final List<String> args;

  public Command(Commands command, List<String> args) {
    this.command = command;
    this.args = args;
  }

  public Command(RespArray commandRespArray) {
    List<RespType<?>> test = commandRespArray.getValue();
    this.command = Commands.from(test.get(0).toString());
    this.args = test.subList(1, test.size()).stream().map(Object::toString).collect(
        Collectors.toList());
  }

  public RespArray getCommandRespArray() {
    List<RespType<?>> arrayArgs = new ArrayList<>();
    arrayArgs.add(new RespBulkString(this.command.toString()));
    this.args.forEach(arg -> arrayArgs.add(new RespBulkString(arg)));
    return new RespArray(arrayArgs);
  }
}

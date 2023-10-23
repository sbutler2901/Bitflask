package dev.sbutler.bitflask.server.command;

import static com.google.common.truth.Truth.assertThat;

import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ServerPingCommand}. */
public class ServerPingCommandTest {

  @Test
  void execute() {
    ServerPingCommand serverPingCommand = new ServerPingCommand();

    ClientCommandResults result = serverPingCommand.execute();

    assertThat(result).isInstanceOf(ClientCommandResults.Success.class);
    assertThat(((ClientCommandResults.Success) result).message()).isEqualTo("pong");
  }
}

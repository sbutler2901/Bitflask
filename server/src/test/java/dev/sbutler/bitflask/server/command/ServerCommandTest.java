package dev.sbutler.bitflask.server.command;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ServerCommand}. */
public class ServerCommandTest {

  @Test
  public void storageCommand_execute() {
    ClientCommand clientCommand = mock(ClientCommand.class);
    ServerCommand command = new ServerCommand.StorageCommand(clientCommand);
    ClientCommandResults mockResults = new ClientCommandResults.Success("test");
    when(clientCommand.execute()).thenReturn(mockResults);

    ClientCommandResults result = command.execute();

    assertThat(result).isEqualTo(mockResults);
  }

  @Test
  public void pingCommand_execute() {
    ServerCommand command = new ServerCommand.PingCommand();

    ClientCommandResults result = command.execute();

    assertThat(result).isInstanceOf(ClientCommandResults.Success.class);
    assertThat(((ClientCommandResults.Success) result).message()).isEqualTo("pong");
  }
}

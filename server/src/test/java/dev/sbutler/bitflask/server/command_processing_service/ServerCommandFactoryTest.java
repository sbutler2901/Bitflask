package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ServerCommandFactory}. */
public class ServerCommandFactoryTest {

  private final ClientCommandFactory clientCommandFactory = mock(ClientCommandFactory.class);

  private final ServerCommandFactory serverCommandFactory =
      new ServerCommandFactory(clientCommandFactory);

  @Test
  void ping() {
    // Act
    ServerCommand serverCommand =
        serverCommandFactory.createCommand(ServerCommandType.PING, ImmutableList.of());
    // Assert
    assertThat(serverCommand).isInstanceOf(ServerPingCommand.class);
  }

  @Test
  void ping_invalid() {
    // Act
    InvalidCommandException e =
        assertThrows(
            InvalidCommandException.class,
            () ->
                serverCommandFactory.createCommand(
                    ServerCommandType.PING, ImmutableList.of("invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("ping");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }

  @Test
  void get() {
    // Act
    ServerCommand serverCommand =
        serverCommandFactory.createCommand(ServerCommandType.GET, ImmutableList.of("key"));
    // Assert
    assertThat(serverCommand).isInstanceOf(ServerStorageCommand.class);
  }

  @Test
  void get_invalid() {
    // Act
    InvalidCommandException e =
        assertThrows(
            InvalidCommandException.class,
            () ->
                serverCommandFactory.createCommand(
                    ServerCommandType.GET, ImmutableList.of("key", "invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("get");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }

  @Test
  void set() {
    // Act
    ServerCommand serverCommand =
        serverCommandFactory.createCommand(ServerCommandType.SET, ImmutableList.of("key", "value"));
    // Assert
    assertThat(serverCommand).isInstanceOf(ServerStorageCommand.class);
  }

  @Test
  void set_invalid() {
    // Act
    InvalidCommandException e =
        assertThrows(
            InvalidCommandException.class,
            () ->
                serverCommandFactory.createCommand(
                    ServerCommandType.SET, ImmutableList.of("key", "value", "invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("set");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }

  @Test
  void delete() {
    // Act
    ServerCommand serverCommand =
        serverCommandFactory.createCommand(ServerCommandType.DEL, ImmutableList.of("key"));
    // Assert
    assertThat(serverCommand).isInstanceOf(ServerStorageCommand.class);
  }

  @Test
  void delete_invalid() {
    // Act
    InvalidCommandException e =
        assertThrows(
            InvalidCommandException.class,
            () ->
                serverCommandFactory.createCommand(
                    ServerCommandType.DEL, ImmutableList.of("key", "invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("del");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }
}

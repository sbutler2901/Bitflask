package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandFactoryTest {

  @InjectMocks
  private CommandFactory commandFactory;
  @Mock
  private ListeningExecutorService listeningExecutorService;
  @Mock
  private StorageCommandDispatcher storageCommandDispatcher;


  @Test
  void ping() {
    // Act
    ServerCommand serverCommand =
        commandFactory.createCommand(CommandType.PING, ImmutableList.of());
    // Assert
    assertThat(serverCommand).isInstanceOf(PingCommand.class);
  }

  @Test
  void ping_invalid() {
    // Act
    InvalidCommandArgumentsException e =
        assertThrows(InvalidCommandArgumentsException.class,
            () -> commandFactory.createCommand(CommandType.PING, ImmutableList.of("invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("ping");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }

  @Test
  void get() {
    // Act
    ServerCommand serverCommand =
        commandFactory.createCommand(CommandType.GET, ImmutableList.of("key"));
    // Assert
    assertThat(serverCommand).isInstanceOf(GetCommand.class);
  }

  @Test
  void get_invalid() {
    // Act
    InvalidCommandArgumentsException e =
        assertThrows(InvalidCommandArgumentsException.class,
            () -> commandFactory.createCommand(CommandType.GET,
                ImmutableList.of("key", "invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("get");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }

  @Test
  void set() {
    // Act
    ServerCommand serverCommand =
        commandFactory.createCommand(CommandType.SET, ImmutableList.of("key", "value"));
    // Assert
    assertThat(serverCommand).isInstanceOf(SetCommand.class);
  }

  @Test
  void set_invalid() {
    // Act
    InvalidCommandArgumentsException e =
        assertThrows(InvalidCommandArgumentsException.class,
            () -> commandFactory.createCommand(CommandType.SET,
                ImmutableList.of("key", "value", "invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("set");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }

  @Test
  void delete() {
    // Act
    ServerCommand serverCommand =
        commandFactory.createCommand(CommandType.DEL, ImmutableList.of("key"));
    // Assert
    assertThat(serverCommand).isInstanceOf(DeleteCommand.class);
  }

  @Test
  void delete_invalid() {
    // Act
    InvalidCommandArgumentsException e =
        assertThrows(InvalidCommandArgumentsException.class,
            () -> commandFactory.createCommand(CommandType.DEL,
                ImmutableList.of("key", "invalidArg")));
    // Assert
    assertThat(e).hasMessageThat().ignoringCase().contains("del");
    assertThat(e).hasMessageThat().ignoringCase().contains("invalidArg");
  }
}

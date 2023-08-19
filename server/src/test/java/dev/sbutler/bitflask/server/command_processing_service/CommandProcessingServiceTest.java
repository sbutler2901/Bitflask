package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link CommandProcessingService}. */
@ExtendWith(MockitoExtension.class)
public class CommandProcessingServiceTest {

  @InjectMocks private CommandProcessingService commandProcessingService;
  @Mock private ServerCommandFactory serverCommandFactory;

  @Test
  void executesCommand() {
    ImmutableList<String> message = ImmutableList.of("ping");
    ServerCommand serverCommand = mock(ServerCommand.class);
    when(serverCommandFactory.createCommand(any(), any())).thenReturn(serverCommand);
    var expectedResult = new ClientCommandResults.Success("pong");
    when(serverCommand.execute()).thenReturn(expectedResult);

    String result = commandProcessingService.processCommandMessage(message);

    assertThat(result).isEqualTo(expectedResult);
  }

  @Test
  void invalidSizeCommandMessage() {
    ImmutableList<String> message = ImmutableList.of();

    InvalidCommandException exception =
        assertThrows(
            InvalidCommandException.class,
            () -> commandProcessingService.processCommandMessage(message));

    assertThat(exception).hasMessageThat().isEqualTo("Message must contain at least one argument");
  }

  @Test
  void invalidCommand() {
    ImmutableList<String> message = ImmutableList.of("invalidCommand");

    InvalidCommandException exception =
        assertThrows(
            InvalidCommandException.class,
            () -> commandProcessingService.processCommandMessage(message));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(String.format("Invalid command [%s]", "invalidCommand"));
  }
}

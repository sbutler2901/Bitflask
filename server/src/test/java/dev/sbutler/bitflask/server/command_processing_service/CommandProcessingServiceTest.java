package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandProcessingServiceTest {

  @InjectMocks
  private CommandProcessingService commandProcessingService;
  @Mock
  private CommandFactory commandFactory;

  @Test
  void executesCommand() {
    // Arrange
    ImmutableList<String> message = ImmutableList.of("ping");
    ServerCommand serverCommand = mock(ServerCommand.class);
    when(commandFactory.createCommand(any(), any())).thenReturn(serverCommand);
    ListenableFuture<String> executeFuture = immediateFuture("pong");
    when(serverCommand.execute()).thenReturn(executeFuture);
    // Act
    ListenableFuture<String> responseFuture = commandProcessingService.processCommandMessage(
        message);
    // Assert
    assertThat(responseFuture).isEqualTo(executeFuture);
  }

  @Test
  void invalidSizeCommandMessage() throws Exception {
    // Arrange
    ImmutableList<String> message = ImmutableList.of();
    // Act
    ListenableFuture<String> responseFuture = commandProcessingService.processCommandMessage(
        message);
    // Assert
    assertThat(responseFuture.isDone()).isTrue();
    assertThat(responseFuture.get()).ignoringCase()
        .contains("at least one argument");
  }

  @Test
  void invalidCommand() throws Exception {
    // Arrange
    ImmutableList<String> message = ImmutableList.of("invalidCommand");
    // Act
    ListenableFuture<String> responseFuture =
        commandProcessingService.processCommandMessage(message);
    // Assert
    assertThat(responseFuture.isDone()).isTrue();
    assertThat(responseFuture.get()).ignoringCase()
        .contains("invalid command");
  }

  @Test
  void commandFactory_throwsInvalidCommandArgumentsException() throws Exception {
    // Arrange
    ImmutableList<String> message = ImmutableList.of("ping", "invalidArg");
    when(commandFactory.createCommand(any(), any())).thenThrow(
        new InvalidCommandArgumentsException("test"));
    // Act
    ListenableFuture<String> responseFuture =
        commandProcessingService.processCommandMessage(message);
    // Assert
    assertThat(responseFuture.isDone()).isTrue();
    assertThat(responseFuture.get()).ignoringCase()
        .contains("test");
  }
}

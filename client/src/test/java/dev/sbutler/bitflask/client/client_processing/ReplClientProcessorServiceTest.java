package dev.sbutler.bitflask.client.client_processing;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplIOException;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplParser;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplReader;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplClientProcessorServiceTest {

  @InjectMocks
  ReplClientProcessorService replClientProcessorService;
  @Mock
  ClientProcessor clientProcessor;
  @Mock
  ReplReader replReader;
  @Mock
  OutputWriter outputWriter;

  @Test
  void inputParser_nullClientInput() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenReturn(null);
      // Act
      assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
      // Assert
      verify(outputWriter, times(1)).write(anyString());
      verify(replReader, times(1)).close();
    }
  }

  @Test
  void clientProcessor_emptyInput() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenReturn(ImmutableList.of())
          .thenReturn(ImmutableList.of());
      when(clientProcessor.processClientInput(any()))
          .thenReturn(true)
          .thenReturn(false);
      // Act
      assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
      // Assert
      verify(outputWriter, times(2)).write(anyString());
    }
  }

  @Test
  void inputParser_throwsReplSyntaxException() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenThrow(IOException.class)
          .thenReturn(ImmutableList.of());
      when(clientProcessor.processClientInput(any()))
          .thenReturn(false);
      // Act
      assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
      verify(outputWriter, times(2)).write(anyString());
    }
  }

  @Test
  void inputParser_throwsReplIOException() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenThrow(ReplIOException.class);
      // Act
      assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
      // Assert
      verify(outputWriter, times(1)).write(anyString());
      verify(replReader, times(1)).close();
    }
  }

  @Test
  void triggerShutdown() throws Exception {
    // Act
    replClientProcessorService.triggerShutdown();
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(0)).writeWithNewLine(anyString());
    verify(outputWriter, times(0)).write(anyString());
    verify(replReader, times(1)).close();
  }
}

package dev.sbutler.bitflask.client.client_processing;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.ReplIOException;
import dev.sbutler.bitflask.client.client_processing.repl.ReplParser;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import dev.sbutler.bitflask.client.client_processing.repl.ReplSyntaxException;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplClientProcessorServiceTest {

  ReplClientProcessorService replClientProcessorService;
  @Mock
  ClientProcessor clientProcessor;
  @Mock
  ReplReader replReader;
  @Mock
  OutputWriter outputWriter;
  @Mock
  ClientConfigurations configurations;

  @BeforeEach
  void beforeEach() {
    when(configurations.getUsePrompt()).thenReturn(true);
    replClientProcessorService =
        new ReplClientProcessorService(clientProcessor, replReader, outputWriter, configurations);
  }

  @Test
  void replParser_endOfInput() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenReturn(Optional.empty());
      // Act
      replClientProcessorService.run();
      // Assert
      verify(outputWriter, times(1)).write(any());
      verify(replReader, times(1)).close();
    }
  }

  @Test
  void replParser_emptyInput() {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(replReader))
          .thenReturn(Optional.of(ImmutableList.of()))
          .thenReturn(Optional.of(ImmutableList.of()));
      when(clientProcessor.processClientInput(any()))
          .thenReturn(true)
          // artificially terminate
          .thenReturn(false);
      // Act
      replClientProcessorService.run();
      // Assert
      verify(outputWriter, times(2)).write(any());
    }
  }

  @Test
  void replParser_throwsReplSyntaxException_cleanup() {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenThrow(ReplSyntaxException.class)
          // artificially terminate
          .thenReturn(Optional.empty());
      // Act
      replClientProcessorService.run();
      // Assert
      replParserMockedStatic.verify(() -> ReplParser.cleanupForNextLine(any()), times(1));
      verify(outputWriter, times(2)).write(any());
      verify(outputWriter, times(1)).writeWithNewLine(any());
    }
  }

  @Test
  void replParser_throwsReplSyntaxException_cleanup_throwsReplIOException() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenThrow(ReplSyntaxException.class);
      replParserMockedStatic.when(() -> ReplParser.cleanupForNextLine(any()))
          .thenThrow(ReplIOException.class);
      // Act
      replClientProcessorService.run();
      // Assert
      replParserMockedStatic.verify(() -> ReplParser.cleanupForNextLine(any()), times(1));
      verify(outputWriter, times(1)).write(any());
      verify(outputWriter, times(2)).writeWithNewLine(any());
      verify(replReader, times(1)).close();
    }
  }

  @Test
  void replParser_throwsReplIOException() throws Exception {
    try (MockedStatic<ReplParser> replParserMockedStatic = mockStatic(ReplParser.class)) {
      // Arrange
      replParserMockedStatic.when(() -> ReplParser.readNextLine(any()))
          .thenThrow(ReplIOException.class);
      // Act
      replClientProcessorService.run();
      // Assert
      verify(outputWriter, times(1)).write(any());
      verify(outputWriter, times(1)).writeWithNewLine(any());
      verify(replReader, times(1)).close();
    }
  }

  @Test
  void triggerShutdown() throws Exception {
    // Act
    replClientProcessorService.triggerShutdown();
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(0)).writeWithNewLine(any());
    verify(outputWriter, times(0)).write(any());
    verify(replReader, times(1)).close();
  }

  @Test
  void triggerShutdown_replReader_throwsIOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(replReader).close();
    // Act
    replClientProcessorService.triggerShutdown();
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(0)).writeWithNewLine(any());
    verify(outputWriter, times(0)).write(any());
    verify(replReader, times(1)).close();
  }
}

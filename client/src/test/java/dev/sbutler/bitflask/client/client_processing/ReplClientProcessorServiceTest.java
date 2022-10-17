package dev.sbutler.bitflask.client.client_processing;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReplClientProcessorServiceTest {

  @InjectMocks
  ReplClientProcessorService replClientProcessorService;
  @Mock
  ClientProcessor clientProcessor;
  @Mock
  InputParser inputParser;
  @Mock
  OutputWriter outputWriter;

  @Test
  void clientProcessor_shouldContinueProcessing() throws Exception {
    // Arrange
    doReturn(ImmutableList.of(), ImmutableList.of()).when(inputParser).getClientNextInput();
    doReturn(true, false).when(clientProcessor).processClientInput(any());
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
    // Assert
    verify(outputWriter, times(2)).write(anyString());
  }

  @Test
  void inputParser_nullClientInput() throws Exception {
    // Arrange
    doReturn(null).when(inputParser).getClientNextInput();
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
    // Assert
    verify(outputWriter, times(1)).write(anyString());
  }

  @Test
  void inputParser_IOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(inputParser).getClientNextInput();
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
    // Assert
    verify(outputWriter, times(1)).write(anyString());
  }

  @Test
  void inputParser_ParseException() throws Exception {
    // Arrange
    when(inputParser.getClientNextInput())
        .thenThrow(new ParseException("message", 0))
        .thenReturn(ImmutableList.of());
    doReturn(false).when(clientProcessor).processClientInput(any());
    // Act
    assertTimeoutPreemptively(Duration.ofMillis(100), () -> replClientProcessorService.run());
    verify(outputWriter, times(2)).write(anyString());
  }

  @Test
  void triggerShutdown() {
    // Act
    replClientProcessorService.triggerShutdown();
    replClientProcessorService.run();
    // Assert
    verify(outputWriter, times(0)).writeWithNewLine(anyString());
    verify(outputWriter, times(0)).write(anyString());
  }
}

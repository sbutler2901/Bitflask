package dev.sbutler.bitflask.client.client_processing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class StdinInputParserTest {

  private static final InputStream DEFAULT_STDIN = System.in;

  StdinInputParser stdinInputParser;

  @AfterEach
  void afterEach() {
    System.setIn(DEFAULT_STDIN);
  }

  void initParser(String nextLine) {
    System.setIn(new ByteArrayInputStream(nextLine.getBytes(StandardCharsets.UTF_8)));
    stdinInputParser = new StdinInputParser();
  }

  @Test
  void getNextCommand_withArgs() {
    // Arrange
    String nextLine = "get test-key";
    ImmutableList<String> expected = ImmutableList.of("get", "test-key");
    // Act
    initParser(nextLine);
    // Assert
    assertEquals(expected, stdinInputParser.getClientNextInput());
  }

  @Test
  void getNextCommand_withoutArgs() {
    // Arrange
    String nextLine = "get";
    ImmutableList<String> expected = ImmutableList.of("get");
    // Act
    initParser(nextLine);
    // Assert
    assertEquals(expected, stdinInputParser.getClientNextInput());
  }

  @Test
  void getNextCommand_emptyString() {
    // Arrange
    String nextLine = "\n";
    ImmutableList<String> expected = ImmutableList.of();
    // Act
    initParser(nextLine);
    // Assert
    assertEquals(expected, stdinInputParser.getClientNextInput());
  }

  @Test
  void getNextCommand_whitespaceOnly() {
    // Arrange
    String nextLine = " ";
    ImmutableList<String> expected = ImmutableList.of();
    // Act
    initParser(nextLine);
    // Assert
    assertEquals(expected, stdinInputParser.getClientNextInput());
  }

}

package dev.sbutler.bitflask.client.client_processing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  void getClientNextInput_EOF() throws Exception {
    // Arrange
    System.setIn(new ByteArrayInputStream(new byte[0]));
    stdinInputParser = new StdinInputParser();
    // Act
    ImmutableList<String> args = stdinInputParser.getClientNextInput();
    // Assert
    assertNull(args);
  }

  @Test
  void getClientNextInput_withArgs() throws Exception {
    // Arrange
    String nextLine = "get test-key";
    ImmutableList<String> expected = ImmutableList.of("get", "test-key");
    initParser(nextLine);
    // Act
    ImmutableList<String> args = stdinInputParser.getClientNextInput();
    // Assert
    assertEquals(expected, args);
  }

  @Test
  void getClientNextInput_withoutArgs() throws Exception {
    // Arrange
    String nextLine = "get";
    ImmutableList<String> expected = ImmutableList.of("get");
    initParser(nextLine);
    // Act
    ImmutableList<String> args = stdinInputParser.getClientNextInput();
    // Assert
    assertEquals(expected, args);
  }

  @Test
  void getClientNextInput_emptyString() throws Exception {
    // Arrange
    String nextLine = "\n";
    ImmutableList<String> expected = ImmutableList.of();
    initParser(nextLine);
    // Act
    ImmutableList<String> args = stdinInputParser.getClientNextInput();
    // Assert
    assertEquals(expected, args);
  }

  @Test
  void getClientNextInput_whitespaceOnly() throws Exception {
    // Arrange
    String nextLine = " ";
    ImmutableList<String> expected = ImmutableList.of();
    initParser(nextLine);
    // Act
    ImmutableList<String> args = stdinInputParser.getClientNextInput();
    // Assert
    assertEquals(expected, args);
  }
}

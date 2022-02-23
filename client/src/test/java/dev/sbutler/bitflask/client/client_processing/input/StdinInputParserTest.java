package dev.sbutler.bitflask.client.client_processing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
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
    String nextLine = "get test-key";
    initParser(nextLine);
    ClientCommand expected = new ClientCommand("get", List.of("test-key"));

    assertEquals(expected, stdinInputParser.getNextCommand());
  }

  @Test
  void getNextCommand_withoutArgs() {
    String nextLine = "get";
    initParser(nextLine);
    ClientCommand expected = new ClientCommand("get", new ArrayList<>());

    assertEquals(expected, stdinInputParser.getNextCommand());
  }

  @Test
  void getNextCommand_emptyString() {
    String nextLine = "\n";
    initParser(nextLine);

    assertNull(stdinInputParser.getNextCommand());
  }

  @Test
  void getNextCommand_whitespaceOnly() {
    String nextLine = " ";
    initParser(nextLine);

    assertNull(stdinInputParser.getNextCommand());
  }

}

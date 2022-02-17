package dev.sbutler.bitflask.client.repl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class InputParserTest {

  @InjectMocks
  InputParser inputParser;

  @Mock
  Scanner inputScanner;

  @Test
  void getNextCommand_withArgs() {
    String nextLine = "get test-key";
    ClientCommand expected = new ClientCommand("get", List.of("test-key"));
    doReturn(nextLine).when(inputScanner).nextLine();
    assertEquals(expected, inputParser.getNextCommand());
  }

  @Test
  void getNextCommand_withoutArgs() {
    String nextLine = "get";
    ClientCommand expected = new ClientCommand("get", new ArrayList<>());
    doReturn(nextLine).when(inputScanner).nextLine();
    assertEquals(expected, inputParser.getNextCommand());
  }

  @Test
  void getNextCommand_emptyString() {
    String nextLine = "";
    doReturn(nextLine).when(inputScanner).nextLine();
    assertNull(inputParser.getNextCommand());
  }

  @Test
  void getNextCommand_whitespaceOnly() {
    String nextLine = " ";
    doReturn(nextLine).when(inputScanner).nextLine();
    assertNull(inputParser.getNextCommand());
  }

}

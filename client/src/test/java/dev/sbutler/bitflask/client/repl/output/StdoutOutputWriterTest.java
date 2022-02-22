package dev.sbutler.bitflask.client.repl.output;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StdoutOutputWriterTest {

  private static final PrintStream DEFAULT_STDOUT = System.out;

  StdoutOutputWriter stdoutOutputWriter;
  PrintStream printStream;

  @AfterEach
  void afterEach() {
    System.setOut(DEFAULT_STDOUT);
  }

  void initWriter() {
    printStream = mock(PrintStream.class);
    System.setOut(printStream);
    stdoutOutputWriter = new StdoutOutputWriter();
  }

  @Test
  void write() {
    String output = "test-output";
    initWriter();
    stdoutOutputWriter.write(output);

    verify(printStream, times(1)).print(anyString());
  }

  @Test
  void writeWithNewLine() {
    String output = "test-output";
    initWriter();
    stdoutOutputWriter.writeWithNewLine(output);

    verify(printStream, times(1)).println(anyString());
  }

}

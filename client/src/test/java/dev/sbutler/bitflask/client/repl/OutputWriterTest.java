package dev.sbutler.bitflask.client.repl;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutputWriterTest {

  @InjectMocks
  OutputWriter outputWriter;

  @Mock
  PrintStream printStream;

  @Test
  void write() {
    String output = "test-output";
    outputWriter.write(output);
    verify(printStream, times(1)).print(anyString());
  }

  @Test
  void writeWithNewLine() {
    String output = "test-output";
    outputWriter.writeWithNewLine(output);
    verify(printStream, times(1)).println(anyString());
  }

}

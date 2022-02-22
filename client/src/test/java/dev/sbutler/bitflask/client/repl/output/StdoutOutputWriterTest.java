package dev.sbutler.bitflask.client.repl.output;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.client.repl.output.StdoutOutputWriter;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StdoutOutputWriterTest {

  @InjectMocks
  StdoutOutputWriter stdoutOutputWriter;

  @Mock
  PrintStream printStream;

  @Test
  void write() {
    String output = "test-output";
    stdoutOutputWriter.write(output);
    verify(printStream, times(1)).print(anyString());
  }

  @Test
  void writeWithNewLine() {
    String output = "test-output";
    stdoutOutputWriter.writeWithNewLine(output);
    verify(printStream, times(1)).println(anyString());
  }

}

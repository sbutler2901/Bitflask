package dev.sbutler.bitflask.client.repl;

import java.io.PrintStream;

public class StdoutOutputWriter implements OutputWriter {

  PrintStream printStream;

  public StdoutOutputWriter(PrintStream printStream) {
    this.printStream = printStream;
  }

  public void write(String output) {
    printStream.print(output);
  }

  public void writeWithNewLine(String output) {
    printStream.println(output);
  }
}

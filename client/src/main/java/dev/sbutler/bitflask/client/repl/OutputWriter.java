package dev.sbutler.bitflask.client.repl;

import java.io.PrintStream;

public class OutputWriter {

  PrintStream printStream;

  public OutputWriter() {
    printStream = System.out;
  }

  public OutputWriter(PrintStream printStream) {
    this.printStream = printStream;
  }

  public void write(String output) {
    printStream.print(output);
  }

  public void writeWithNewLine(String output) {
    printStream.println(output);
  }
}

package dev.sbutler.bitflask.client.client_processing.output;

import java.io.PrintStream;

public class StdoutOutputWriter implements OutputWriter {

  PrintStream printStream;

  public StdoutOutputWriter() {
    this.printStream = System.out;
  }

  public void write(String output) {
    printStream.print(output);
  }

  public void writeWithNewLine(String output) {
    printStream.println(output);
  }
}

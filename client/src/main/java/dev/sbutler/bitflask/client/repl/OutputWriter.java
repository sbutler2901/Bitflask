package dev.sbutler.bitflask.client.repl;

public class OutputWriter {

  public OutputWriter() {

  }

  public void write(String output) {
    System.out.print(output);
  }

  public void writeWithNewLine(String output) {
    System.out.println(output);
  }
}

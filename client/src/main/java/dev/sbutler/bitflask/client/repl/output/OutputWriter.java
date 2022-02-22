package dev.sbutler.bitflask.client.repl.output;

public interface OutputWriter {

  void write(String output);

  void writeWithNewLine(String output);

}

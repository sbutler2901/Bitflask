package dev.sbutler.bitflask.client.repl;

public interface OutputWriter {

  void write(String output);

  void writeWithNewLine(String output);

}

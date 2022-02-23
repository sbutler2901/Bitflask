package dev.sbutler.bitflask.client.client_processing.output;

public interface OutputWriter {

  void write(String output);

  void writeWithNewLine(String output);

}

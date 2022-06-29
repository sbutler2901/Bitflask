package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.file.Path;

interface SegmentFile {

  void write(byte[] data, long fileOffset) throws IOException;

  byte[] read(int readLength, long fileOffset) throws IOException;

  String readAsString(int readLength, long fileOffset) throws IOException;

  byte readByte(long fileOffset) throws IOException;

  long size() throws IOException;

  Path getSegmentFilePath();

  int getSegmentFileKey();

  void close();

  boolean isOpen();

}

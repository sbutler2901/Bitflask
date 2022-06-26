package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

class SegmentFile {

  private final FileChannel segmentFileChannel;
  private final Path segmentFilePath;
  private final int segmentFileKey;

  public SegmentFile(FileChannel segmentFileChannel, Path segmentFilePath,
      int segmentFileKey) {
    this.segmentFileChannel = segmentFileChannel;
    this.segmentFilePath = segmentFilePath;
    this.segmentFileKey = segmentFileKey;
  }

  void write(byte[] data, long fileOffset) throws IOException {
    segmentFileChannel.write(ByteBuffer.wrap(data), fileOffset);
  }

  byte[] read(int readLength, long fileOffset) throws IOException {
    ByteBuffer readBytesBuffer = ByteBuffer.allocate(readLength);
    segmentFileChannel.read(readBytesBuffer, fileOffset);
    return readBytesBuffer.array();
  }

  String readAsString(int readLength, long fileOffset) throws IOException {
    return new String(read(readLength, fileOffset));
  }

  byte readByte(long fileOffset) throws IOException {
    byte[] readBytes = read(1, fileOffset);
    return readBytes[0];
  }

  long size() throws IOException {
    return segmentFileChannel.size();
  }

  Path getSegmentFilePath() {
    return segmentFilePath;
  }

  int getSegmentFileKey() {
    return segmentFileKey;
  }

  void close() {
    try {
      segmentFileChannel.close();
    } catch (IOException ignored) {
    }
  }

  boolean isOpen() {
    return segmentFileChannel.isOpen();
  }

}

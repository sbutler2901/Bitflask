package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import javax.inject.Inject;

final class SegmentFile {

  /**
   * Factory for creating new SegmentFile instances.
   */
  static class Factory {

    @Inject
    Factory() {
    }

    /**
     * Creates a SegmentFile.
     *
     * @param segmentFileChannel File channel for storing segment data
     * @param segmentFilePath    path of the segment file
     * @param segmentFileKey     key of the segment file
     * @return the created SegmentFile
     */
    SegmentFile create(FileChannel segmentFileChannel, Path segmentFilePath, int segmentFileKey) {
      return new SegmentFile(segmentFileChannel, segmentFilePath, segmentFileKey);
    }
  }

  private final FileChannel segmentFileChannel;
  private final Path segmentFilePath;
  private final int segmentFileKey;

  private SegmentFile(FileChannel segmentFileChannel, Path segmentFilePath, int segmentFileKey) {
    this.segmentFileChannel = segmentFileChannel;
    this.segmentFilePath = segmentFilePath;
    this.segmentFileKey = segmentFileKey;
  }

  public void write(byte[] data, long fileOffset) throws IOException {
    segmentFileChannel.write(ByteBuffer.wrap(data), fileOffset);
  }

  public byte[] read(int readLength, long fileOffset) throws IOException {
    ByteBuffer readBytesBuffer = ByteBuffer.allocate(readLength);
    segmentFileChannel.read(readBytesBuffer, fileOffset);
    return readBytesBuffer.array();
  }

  public String readAsString(int readLength, long fileOffset) throws IOException {
    return new String(read(readLength, fileOffset));
  }

  public byte readByte(long fileOffset) throws IOException {
    byte[] readBytes = read(1, fileOffset);
    return readBytes[0];
  }

  public long size() throws IOException {
    return segmentFileChannel.size();
  }

  public Path getSegmentFilePath() {
    return segmentFilePath;
  }

  public int getSegmentFileKey() {
    return segmentFileKey;
  }

  public void close() {
    try {
      segmentFileChannel.close();
    } catch (IOException ignored) {
    }
  }

  public boolean isOpen() {
    return segmentFileChannel.isOpen();
  }

}

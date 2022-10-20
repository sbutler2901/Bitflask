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
     * @param header             The segment files' header defining its file specific attributes
     * @return the created SegmentFile
     */
    SegmentFile create(FileChannel segmentFileChannel, Path segmentFilePath, Header header) {
      return new SegmentFile(segmentFileChannel, segmentFilePath, header);
    }
  }

  private final FileChannel segmentFileChannel;
  private final Path segmentFilePath;
  private final Header header;

  private SegmentFile(FileChannel segmentFileChannel, Path segmentFilePath, Header header) {
    this.segmentFileChannel = segmentFileChannel;
    this.segmentFilePath = segmentFilePath;
    this.header = header;
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

  public void truncate(long size) throws IOException {
    segmentFileChannel.truncate(size);
  }

  public Path getSegmentFilePath() {
    return segmentFilePath;
  }

  public int getSegmentFileKey() {
    return header.key();
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


  /**
   * The header at the start of every SegmentFile defining the file's attributes.
   *
   * @param key the Segment's key, represented as a 16-bit char, allowing 2^16 unique segments
   */
  record Header(char key) {

    /**
     * The number of bytes required to represent a Header
     */
    static final int NUM_BYTES = Character.BYTES;

    /**
     * Converts the header into a {@link ByteBuffer}
     */
    ByteBuffer getHeaderAsByteBuffer() {
      ByteBuffer byteBuffer = ByteBuffer.allocate(NUM_BYTES);
      byteBuffer.putChar(key);
      return byteBuffer;
    }

    /**
     * Reads the provided {@link ByteBuffer} and converts its contents into a Header.
     *
     * <p>Note this method affects the buffer's current position.
     */
    static Header createFromByteBuffer(ByteBuffer byteBuffer) {
      if (byteBuffer.capacity() < NUM_BYTES) {
        throw new IllegalArgumentException(
            "The provided ByteBuffer does not have adequate capacity to represent a Header");
      }
      byteBuffer.rewind();
      char key = byteBuffer.getChar();
      return new Header(key);
    }
  }
}

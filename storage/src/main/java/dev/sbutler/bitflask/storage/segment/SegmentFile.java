package dev.sbutler.bitflask.storage.segment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

class SegmentFile {

  private final AsynchronousFileChannel segmentFileChannel;

  public SegmentFile(AsynchronousFileChannel segmentFileChannel) {
    this.segmentFileChannel = segmentFileChannel;
  }

  void write(byte[] data, long fileOffset) throws IOException {
    Future<Integer> writeFuture = segmentFileChannel.write(ByteBuffer.wrap(data), fileOffset);
    try {
      writeFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      String message = String.format("Failed to write the provided date at fileOffset [%d]",
          fileOffset);
      throw new IOException(message, e);
    }
  }

  byte[] read(int readLength, long fileOffset) throws IOException {
    ByteBuffer readBytesBuffer = ByteBuffer.allocate(readLength);
    Future<Integer> readFuture = segmentFileChannel.read(readBytesBuffer, fileOffset);

    try {
      readFuture.get();
    } catch (InterruptedException | ExecutionException e) {
      String message = String.format("Failed to read the date at fileOffset [%d]", fileOffset);
      throw new IOException(message, e);
    }

    return readBytesBuffer.array();
  }

  long size() throws IOException {
    return segmentFileChannel.size();
  }

}

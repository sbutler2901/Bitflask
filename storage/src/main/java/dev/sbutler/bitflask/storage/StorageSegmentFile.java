package dev.sbutler.bitflask.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

class StorageSegmentFile {

  private static final String DEFAULT_SEGMENT_FILE_PATH = "store/segment%d.txt";
  private static final StandardOpenOption[] fileOptions = {StandardOpenOption.CREATE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
  private static final Set<StandardOpenOption> fileChannelOptions = new HashSet<>(
      Arrays.asList(fileOptions));

  private final AsynchronousFileChannel segmentFileChannel;

  public StorageSegmentFile(ExecutorService executorService, int segmentIndex)
      throws IOException {
    Path newSegmentFilePath = Paths
        .get(String.format(DEFAULT_SEGMENT_FILE_PATH, segmentIndex));
    segmentFileChannel = AsynchronousFileChannel
        .open(newSegmentFilePath, fileChannelOptions, executorService);
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
}

package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

class SegmentFactoryImpl implements SegmentFactory {

  private static final String DEFAULT_SEGMENT_FILE_PATH = "store/segment_%s.txt";
  private static final Set<StandardOpenOption> fileChannelOptions = Sets.newHashSet(
      StandardOpenOption.CREATE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE
//      StandardOpenOption.TRUNCATE_EXISTING
  );

  ExecutorService executorService;
  private final AtomicInteger nextSegmentIndex = new AtomicInteger(0);

  @Inject
  SegmentFactoryImpl(@StorageExecutorService ExecutorService executorService) {
    this.executorService = executorService;
  }

  @Override
  public Segment createSegment() throws IOException {
    SegmentFile segmentFile = createSegmentFile();
    return new SegmentImpl(segmentFile);
  }

  private SegmentFile createSegmentFile() throws IOException {
    String segmentIndex = getNextSegmentKey();
    Path segmentPath = getNextSegmentFilePath(segmentIndex);
    FileChannel segmentFileChannel = getNextSegmentFileChannel(segmentPath);
    return new SegmentFile(segmentFileChannel, segmentPath, segmentIndex);
  }

  private String getNextSegmentKey() {
    return String.valueOf(nextSegmentIndex.getAndIncrement());
  }

  private Path getNextSegmentFilePath(String segmentKey) {
    return Paths.get(String.format(DEFAULT_SEGMENT_FILE_PATH, segmentKey));
  }

  private FileChannel getNextSegmentFileChannel(Path nextSegmentFilePath)
      throws IOException {
    return FileChannel.open(nextSegmentFilePath, fileChannelOptions);
  }

  private AsynchronousFileChannel getNextSegmentAsynchronousFileChannel(Path nextSegmentFilePath)
      throws IOException {
    return AsynchronousFileChannel
        .open(nextSegmentFilePath, fileChannelOptions, executorService);
  }

  @Override
  public void setSegmentStartIndex(int segmentStartIndex) {
    nextSegmentIndex.set(segmentStartIndex);
  }

}

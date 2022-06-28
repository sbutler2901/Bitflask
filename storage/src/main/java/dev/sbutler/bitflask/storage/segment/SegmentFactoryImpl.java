package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

class SegmentFactoryImpl implements SegmentFactory {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String DEFAULT_SEGMENT_FILENAME = "%d_segment.txt";
  static final String DEFAULT_SEGMENT_DIR_PATH =
      System.getProperty("user.home") + "/.bitflask/store/";
  static final ImmutableSet<StandardOpenOption> fileChannelOptions = ImmutableSet.of(
      StandardOpenOption.CREATE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE
//      StandardOpenOption.TRUNCATE_EXISTING
  );

  private final AtomicInteger nextSegmentKey = new AtomicInteger(0);

  @Inject
  SegmentFactoryImpl() {
  }

  @Override
  public Segment createSegment() throws IOException {
    SegmentFile segmentFile = createSegmentFile();
    return createSegmentFromFile(segmentFile);
  }

  @Override
  public Segment createSegmentFromFile(SegmentFile segmentFile) throws IOException {
    Segment newSegment = new SegmentImpl(segmentFile);
    logger.atInfo().log("Created new segment with fileKey [%s]", newSegment.getSegmentFileKey());
    return newSegment;
  }

  private SegmentFile createSegmentFile() throws IOException {
    int segmentIndex = getNextSegmentKey();
    Path segmentPath = getNextSegmentFilePath(segmentIndex);
    FileChannel segmentFileChannel = getNextSegmentFileChannel(segmentPath);
    return new SegmentFile(segmentFileChannel, segmentPath, segmentIndex);
  }

  private int getNextSegmentKey() {
    return nextSegmentKey.getAndIncrement();
  }

  private Path getNextSegmentFilePath(int segmentKey) {
    String segmentFilename = String.format(DEFAULT_SEGMENT_FILENAME, segmentKey);
    return Paths.get(DEFAULT_SEGMENT_DIR_PATH, segmentFilename);
  }

  private FileChannel getNextSegmentFileChannel(Path nextSegmentFilePath) throws IOException {
    return FileChannel.open(nextSegmentFilePath, fileChannelOptions);
  }

  @Override
  public void setSegmentStartKey(int segmentStartKey) {
    nextSegmentKey.set(segmentStartKey);
  }

  @Override
  public boolean createSegmentStoreDir() throws IOException {
    Path segmentStoreDirPath = getSegmentStoreDirPath();
    boolean segmentStoreDirExists = Files.isDirectory(segmentStoreDirPath);
    if (!segmentStoreDirExists) {
      Files.createDirectories(getSegmentStoreDirPath());
      logger.atInfo().log("Created segment store directory at [%s]", segmentStoreDirPath);
      return true;
    }
    logger.atInfo().log("Segment store directory already existed at [{}]", segmentStoreDirPath);
    return false;
  }

  @Override
  public Path getSegmentStoreDirPath() {
    return Paths.get(DEFAULT_SEGMENT_DIR_PATH);
  }

  @Override
  public int getSegmentKeyFromPath(Path path) {
    String segmentFileName = path.getFileName().toString();
    int keyEndIndex = segmentFileName.indexOf('_');
    return Integer.parseInt(segmentFileName.substring(0, keyEndIndex));
  }

}

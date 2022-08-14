package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.StorageConfiguration;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class SegmentFactoryImpl {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  static final String DEFAULT_SEGMENT_FILENAME = "%d_segment.txt";
  static final ImmutableSet<StandardOpenOption> fileChannelOptions = ImmutableSet.of(
      StandardOpenOption.CREATE,
      StandardOpenOption.READ,
      StandardOpenOption.WRITE
//      StandardOpenOption.TRUNCATE_EXISTING
  );

  private final SegmentFileFactory segmentFileFactory;
  private final Path storeDirectoryPath;
  private final long segmentSizeLimit;
  private final AtomicInteger nextSegmentKey = new AtomicInteger(0);

  SegmentFactoryImpl(SegmentFileFactory segmentFileFactory,
      StorageConfiguration storageConfiguration) {
    this.segmentFileFactory = segmentFileFactory;
    this.storeDirectoryPath = storageConfiguration.getStorageStoreDirectoryPath();
    this.segmentSizeLimit = storageConfiguration.getStorageSegmentSizeLimit();
  }

  /**
   * Creates a new Segment, and it's associate SegmentFile in the filesystem
   *
   * @return the created Segment
   * @throws IOException if an error occurs while creating the segment
   */
  public Segment createSegment() throws IOException {
    SegmentFile segmentFile = createSegmentFile();
    return createSegmentFromFile(segmentFile);
  }

  /**
   * Creates a segment from a preexisting SegmentFile. This does not increment the key used for
   * creating new Segments.
   *
   * @param segmentFile the associated SegmentFile for the Segment being created
   * @return the created segment
   * @throws IOException if an error occurs while creating the segment
   */
  public Segment createSegmentFromFile(SegmentFile segmentFile) throws IOException {
    AtomicLong currentFileWriteOffset = new AtomicLong(segmentFile.size());
    ConcurrentMap<String, Long> keyedEntryFileOffsetMap = generateKeyedEntryOffsetMap(segmentFile,
        currentFileWriteOffset);

    Segment newSegment = new SegmentImpl(segmentFile, keyedEntryFileOffsetMap,
        currentFileWriteOffset, segmentSizeLimit);
    logger.atInfo().log("Created new segment with fileKey [%s]", newSegment.getSegmentFileKey());
    return newSegment;
  }

  /**
   * Populates the map of keys managed by the new segment and their corresponding offset within the
   * segment's corresponding SegmentFile.
   *
   * @throws IOException if an error occurs while populating the key offset map
   */
  private ConcurrentMap<String, Long> generateKeyedEntryOffsetMap(SegmentFile segmentFile,
      AtomicLong currentFileWriteOffset) throws IOException {
    // todo: handle empty spaces that occur because of failed writes
    ConcurrentMap<String, Long> keyedEntryFileOffsetMap = new ConcurrentHashMap<>();
    long nextOffsetStart = 0;
    while (nextOffsetStart < currentFileWriteOffset.get()) {
      long entryStartOffset = nextOffsetStart;

      // Get key and update offset map
      int keyLength = segmentFile.readByte(nextOffsetStart++);
      String key = segmentFile.readAsString(keyLength, nextOffsetStart);
      nextOffsetStart += keyLength;
      keyedEntryFileOffsetMap.put(key, entryStartOffset);

      // Start next iteration offset after current entry's value
      int valueLength = segmentFile.readByte(nextOffsetStart++);
      nextOffsetStart += valueLength;
    }
    return keyedEntryFileOffsetMap;
  }

  private SegmentFile createSegmentFile() throws IOException {
    int segmentIndex = getNextSegmentKey();
    Path segmentPath = getNextSegmentFilePath(segmentIndex);
    FileChannel segmentFileChannel = getNextSegmentFileChannel(segmentPath);
    return segmentFileFactory.create(segmentFileChannel, segmentPath, segmentIndex);
  }

  private int getNextSegmentKey() {
    return nextSegmentKey.getAndIncrement();
  }

  private Path getNextSegmentFilePath(int segmentKey) {
    String segmentFilename = String.format(DEFAULT_SEGMENT_FILENAME, segmentKey);
    return storeDirectoryPath.resolve(segmentFilename);
  }

  private FileChannel getNextSegmentFileChannel(Path nextSegmentFilePath) throws IOException {
    return FileChannel.open(nextSegmentFilePath, fileChannelOptions);
  }

  /**
   * Sets the key that is used as the file key when creating new Segments. This is incremented with
   * each new Segment while a SegmentFile is created.
   *
   * @param segmentStartKey the key to be used on next segment creation
   */
  public void setSegmentStartKey(int segmentStartKey) {
    nextSegmentKey.set(segmentStartKey);
  }

  /**
   * Creates the segment store directory if it doesn't exist.
   *
   * @return true if the directory was created
   */
  public boolean createSegmentStoreDir() throws IOException {
    boolean segmentStoreDirExists = Files.isDirectory(storeDirectoryPath);
    if (!segmentStoreDirExists) {
      Files.createDirectories(storeDirectoryPath);
      logger.atInfo().log("Created segment store directory at [%s]", storeDirectoryPath);
      return true;
    }
    logger.atInfo().log("Segment store directory already existed at [%s]", storeDirectoryPath);
    return false;
  }

  /**
   * Retrieves the key for a segment from its file path.
   *
   * @param path the path to retrieve the segment key from
   * @return the Segment's Key.
   */
  public int getSegmentKeyFromPath(Path path) {
    String segmentFileName = path.getFileName().toString();
    int keyEndIndex = segmentFileName.indexOf('_');
    return Integer.parseInt(segmentFileName.substring(0, keyEndIndex));
  }
}

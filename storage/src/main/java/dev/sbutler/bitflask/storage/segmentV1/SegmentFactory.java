package dev.sbutler.bitflask.storage.segmentV1;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.segmentV1.Encoder.Header;
import dev.sbutler.bitflask.storage.segmentV1.Encoder.Offsets;
import dev.sbutler.bitflask.storage.segmentV1.Encoder.PartialOffsets;
import dev.sbutler.bitflask.storage.segmentV1.Segment.Entry;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles the creation of new {@link Segment}s.
 *
 * <p>Supports creating a new Segment with a new {@link SegmentFile} or creating a new Segment from
 * a pre-existing SegmentFile.
 *
 * <p>If a Segment is created from a pre-existing SegmentFile the Segment's entry map will be
 * populated from the file.
 *
 * <p>The factory can be configured to load a SegmentFile's contents into the Segment's entry
 * map, or truncate any contents. This applies to SegmentFiles provided when requesting Segment
 * creation and when a SegmentFile is automatically created for a new Segment but a file already
 * exists on the file system.
 */
@Singleton
final class SegmentFactory {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String SEGMENT_FILENAME_SUFFIX = "_segment.txt";
  static final String DEFAULT_SEGMENT_FILENAME = "%d" + SEGMENT_FILENAME_SUFFIX;
  static final ImmutableSet<StandardOpenOption> BASE_FILE_CHANNEL_OPTIONS = ImmutableSet.of(
      StandardOpenOption.READ,
      StandardOpenOption.WRITE);

  private final ImmutableSet<StandardOpenOption> fileChannelOptions;
  private final SegmentFile.Factory segmentFileFactory;
  private final Path storeDirectoryPath;
  private final long segmentSizeLimit;
  private final AtomicInteger nextSegmentKey = new AtomicInteger(0);

  @Inject
  SegmentFactory(SegmentFile.Factory segmentFileFactory,
      StorageConfigurations storageConfigurations) {
    this.segmentFileFactory = segmentFileFactory;
    this.storeDirectoryPath = storageConfigurations.getStorageStoreDirectoryPath();
    this.segmentSizeLimit = storageConfigurations.getStorageSegmentSizeLimit();
    this.fileChannelOptions = determineFileChannelOptions(storageConfigurations);
  }

  private ImmutableSet<StandardOpenOption> determineFileChannelOptions(
      StorageConfigurations storageConfigurations) {
    return ImmutableSet.<StandardOpenOption>builder()
        .addAll(BASE_FILE_CHANNEL_OPTIONS)
        .add(storageConfigurations.getStorageSegmentCreationMode())
        .build();
  }

  public static boolean isValidSegmentFilePath(Path path) {
    return path.getFileName().toString().contains(SEGMENT_FILENAME_SUFFIX);
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
   * Creates a segment from a preexisting SegmentFile.
   *
   * @param segmentFile the associated SegmentFile for the Segment being created
   * @return the created segment
   * @throws IOException if an error occurs while creating the segment
   */
  public Segment createSegmentFromFile(SegmentFile segmentFile) throws IOException {
    if (shouldTruncateFile()) {
      segmentFile.truncate(0);
    }
    // Update next segment key, if needed.
    nextSegmentKey.getAndUpdate(current -> {
      int fileKey = segmentFile.getSegmentFileKey();
      if (fileKey >= current) {
        return fileKey + 1;
      }
      return current;
    });

    AtomicLong currentFileWriteOffset = new AtomicLong(segmentFile.size());
    ConcurrentMap<String, Entry> keyedEntryMap =
        generateKeyedEntryMap(segmentFile, currentFileWriteOffset);

    Segment newSegment = new Segment(segmentFile, keyedEntryMap,
        currentFileWriteOffset, segmentSizeLimit);
    logger.atInfo().log("Created Segment with fileKey [%s]", newSegment.getSegmentFileKey());
    return newSegment;
  }


  /**
   * Populates the map of keys managed by the new segment and their corresponding offset within the
   * segment's corresponding SegmentFile.
   *
   * @throws IOException if an error occurs while populating the key offset map
   */
  private ConcurrentMap<String, Entry> generateKeyedEntryMap(SegmentFile segmentFile,
      AtomicLong currentFileWriteOffset) throws IOException {
    // TODO: handle empty spaces that occur because of failed writes
    ConcurrentMap<String, Entry> keyedEntryMap = new ConcurrentHashMap<>();
    long nextEntryOffset = SegmentFile.getFirstEntryOffset();
    while (nextEntryOffset < currentFileWriteOffset.get()) {
      long currentEntryOffset = nextEntryOffset;
      PartialOffsets partialOffsets = Encoder.decodePartial(currentEntryOffset);

      // Get offsets and key
      int keyLength = segmentFile.readByte(partialOffsets.keyLength());
      Offsets offsets = Encoder.decode(currentEntryOffset, keyLength);
      String key = segmentFile.readAsString(keyLength, offsets.key());

      // Create and insert keyed entry
      byte headerByte = segmentFile.readByte(offsets.header());
      Header header = Header.byteToHeaderMapper(headerByte);
      Entry entry = new Entry(header, currentEntryOffset);
      keyedEntryMap.put(key, entry);

      // Start next iteration offset after current entry's value
      int valueLength = segmentFile.readByte(offsets.valueLength());
      nextEntryOffset = Encoder.getNextOffsetAfterEntity(offsets, valueLength);
    }
    return keyedEntryMap;
  }

  private SegmentFile createSegmentFile() throws IOException {
    // Get SegmentFile's properties and create dependencies
    int segmentIndex = nextSegmentKey.getAndIncrement();
    Path segmentPath = getNextSegmentFilePath(segmentIndex);
    FileChannel segmentFileChannel = FileChannel.open(segmentPath, fileChannelOptions);

    // Write SegmentFile's header
    SegmentFile.Header header = new SegmentFile.Header(segmentIndex);
    header.writeToFileChannel(segmentFileChannel);

    logger.atInfo().log("Creating new SegmentFile at [%s]", segmentPath);
    return segmentFileFactory.create(segmentFileChannel, segmentPath, header);
  }

  private Path getNextSegmentFilePath(int segmentKey) {
    String segmentFilename = String.format(DEFAULT_SEGMENT_FILENAME, segmentKey);
    return storeDirectoryPath.resolve(segmentFilename);
  }

  public ImmutableSet<StandardOpenOption> getFileChannelOptions() {
    return fileChannelOptions;
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

  private boolean shouldTruncateFile() {
    return fileChannelOptions.contains(StandardOpenOption.TRUNCATE_EXISTING);
  }
}

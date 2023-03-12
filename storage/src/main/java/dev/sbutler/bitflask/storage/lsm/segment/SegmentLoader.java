package dev.sbutler.bitflask.storage.lsm.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;

public class SegmentLoader {

  private static final String SEGMENT_GLOB = String.format("*.%s", Segment.FILE_EXTENSION);

  private final ThreadFactory threadFactory;
  private final StorageConfigurations configurations;

  @Inject
  SegmentLoader(ThreadFactory threadFactory, StorageConfigurations configurations) {
    this.threadFactory = threadFactory;
    this.configurations = configurations;
  }

  public ImmutableList<Segment> loadWithIndexes(
      ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap) {
    ImmutableList<Path> segmentPaths = LoaderUtils.loadPathsInDirForGlob(
        configurations.getStorageStoreDirectoryPath(), SEGMENT_GLOB);
    // TODO: load metadata and bloom filter from entries
    return ImmutableList.of();
  }
}

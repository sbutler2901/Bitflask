package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import jdk.incubator.concurrent.StructuredTaskScope;

/** Handles loading all {@link Segment}s in the storage directory. */
final class SegmentLoader {

  private static final String SEGMENT_GLOB = String.format("*.%s", Segment.FILE_EXTENSION);

  private final StorageConfig storageConfig;
  private final ThreadFactory threadFactory;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLoader(
      StorageConfig storageConfig, ThreadFactory threadFactory, SegmentFactory segmentFactory) {
    this.storageConfig = storageConfig;
    this.threadFactory = threadFactory;
    this.segmentFactory = segmentFactory;
  }

  /**
   * Loads existing {@link Segment}s in the storage directory and matches them with their
   * corresponding index.
   */
  ImmutableList<Segment> loadWithIndexes(
      ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap) {
    ImmutableList<Path> segmentPaths =
        LoaderUtils.loadPathsInDirForGlob(
            Path.of(storageConfig.getStoreDirectoryPath()), SEGMENT_GLOB);

    try (var scope =
        new StructuredTaskScope.ShutdownOnFailure("load-segment-scope", threadFactory)) {
      List<Future<Segment>> segmentFutures = new ArrayList<>(segmentPaths.size());
      for (var path : segmentPaths) {
        segmentFutures.add(
            scope.fork(() -> segmentFactory.loadFromPath(path, segmentNumberToIndexMap)));
      }

      try {
        scope.join();
        scope.throwIfFailed(e -> new StorageLoadException("Failed loading Segments", e));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageLoadException("Interrupted while loading Segments", e);
      }

      return segmentFutures.stream().map(Future::resultNow).collect(toImmutableList());
    }
  }

  /** Deletes all existing {@link Segment}s in the storage directory. */
  void truncate() {
    LoaderUtils.deletePathsInDirForGlob(
        Path.of(storageConfig.getStoreDirectoryPath()), SEGMENT_GLOB);
  }
}

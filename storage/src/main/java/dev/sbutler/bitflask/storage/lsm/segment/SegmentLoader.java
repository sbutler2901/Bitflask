package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.inject.Inject;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * Handles loading all {@link Segment}s in the storage directory.
 */
final class SegmentLoader {

  private static final String SEGMENT_GLOB = String.format("*.%s", Segment.FILE_EXTENSION);

  private final ThreadFactory threadFactory;
  private final StorageConfigurations configurations;
  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLoader(
      ThreadFactory threadFactory,
      StorageConfigurations configurations,
      SegmentFactory segmentFactory) {
    this.threadFactory = threadFactory;
    this.configurations = configurations;
    this.segmentFactory = segmentFactory;
  }

  public ImmutableList<Segment> loadWithIndexes(
      ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap) {
    ImmutableList<Path> segmentPaths = LoaderUtils.loadPathsInDirForGlob(
        configurations.getStorageStoreDirectoryPath(), SEGMENT_GLOB);
    try (var scope =
        new StructuredTaskScope.ShutdownOnFailure("load-segment-scope", threadFactory)) {
      List<Future<Segment>> segmentFutures = new ArrayList<>(segmentPaths.size());
      for (var path : segmentPaths) {
        segmentFutures.add(scope.fork(
            () -> segmentFactory.loadFromPath(path, segmentNumberToIndexMap)));
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
}

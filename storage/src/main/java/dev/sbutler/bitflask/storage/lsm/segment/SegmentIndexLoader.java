package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
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
 * Handles loading all {@link SegmentIndex} in the storage directory.
 */
final class SegmentIndexLoader {

  private static final String INDEX_GLOB = String.format("*.%s", SegmentIndex.FILE_EXTENSION);

  private final ThreadFactory threadFactory;
  private final StorageConfigurations configurations;
  private final SegmentIndexFactory segmentIndexFactory;

  @Inject
  SegmentIndexLoader(
      ThreadFactory threadFactory,
      StorageConfigurations configurations,
      SegmentIndexFactory segmentIndexFactory) {
    this.threadFactory = threadFactory;
    this.configurations = configurations;
    this.segmentIndexFactory = segmentIndexFactory;
  }

  /**
   * Load all {@link SegmentIndex} in the storage directory.
   */
  ImmutableList<SegmentIndex> load() {
    ImmutableList<Path> indexPaths = LoaderUtils.loadPathsInDirForGlob(
        configurations.getStorageStoreDirectoryPath(), INDEX_GLOB);

    try (var scope = new StructuredTaskScope.ShutdownOnFailure("load-index-scope", threadFactory)) {
      List<Future<SegmentIndex>> indexFutures = new ArrayList<>(indexPaths.size());
      for (var path : indexPaths) {
        indexFutures.add(scope.fork(() -> segmentIndexFactory.loadFromPath(path)));
      }

      try {
        scope.join();
        scope.throwIfFailed(e -> new StorageLoadException("Failed loading SegmentIndexes", e));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageLoadException("Interrupted while loading SegmentIndexes", e);
      }

      return indexFutures.stream().map(Future::resultNow).collect(toImmutableList());
    }
  }
}

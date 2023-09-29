package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils;
import jakarta.inject.Inject;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;

/** Handles loading all {@link SegmentIndex} in the storage directory. */
final class SegmentIndexLoader {

  private static final String INDEX_GLOB = String.format("*.%s", SegmentIndex.FILE_EXTENSION);

  private final StorageConfig storageConfig;
  private final ThreadFactory threadFactory;
  private final SegmentIndexFactory segmentIndexFactory;

  @Inject
  SegmentIndexLoader(
      StorageConfig storageConfig,
      ThreadFactory threadFactory,
      SegmentIndexFactory segmentIndexFactory) {
    this.storageConfig = storageConfig;
    this.threadFactory = threadFactory;
    this.segmentIndexFactory = segmentIndexFactory;
  }

  /** Loads existing {@link SegmentIndex} in the storage directory. */
  ImmutableList<SegmentIndex> load() {
    ImmutableList<Path> indexPaths =
        LoaderUtils.loadPathsInDirForGlob(
            Path.of(storageConfig.getStoreDirectoryPath()), INDEX_GLOB);

    try (var scope = new StructuredTaskScope.ShutdownOnFailure("load-index-scope", threadFactory)) {
      List<StructuredTaskScope.Subtask<SegmentIndex>> indexFutures =
          new ArrayList<>(indexPaths.size());
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

      return indexFutures.stream().map(StructuredTaskScope.Subtask::get).collect(toImmutableList());
    }
  }

  /** Deletes all existing {@link SegmentIndex}s in the storage directory. */
  void truncate() {
    LoaderUtils.deletePathsInDirForGlob(Path.of(storageConfig.getStoreDirectoryPath()), INDEX_GLOB);
  }
}

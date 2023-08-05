package dev.sbutler.bitflask.storage.lsm;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.memtable.MemtableLoader;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMapLoader;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import jdk.incubator.concurrent.StructuredTaskScope;

/** Handles loading all necessary resources at start up for the {@link LSMTree}. */
public final class LSMTreeLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final StorageConfig storageConfig;
  private final ListeningScheduledExecutorService scheduledExecutorService;
  private final ThreadFactory threadFactory;
  private final LSMTreeStateManager stateManager;
  private final LSMTreeCompactor compactor;
  private final MemtableLoader memtableLoader;
  private final SegmentLevelMultiMapLoader segmentLevelMultiMapLoader;

  @Inject
  LSMTreeLoader(
      StorageConfig storageConfig,
      @LSMTreeListeningScheduledExecutorService
          ListeningScheduledExecutorService scheduledExecutorService,
      ThreadFactory threadFactory,
      LSMTreeStateManager stateManager,
      LSMTreeCompactor compactor,
      MemtableLoader memtableLoader,
      SegmentLevelMultiMapLoader segmentLevelMultiMapLoader) {
    this.storageConfig = storageConfig;
    this.scheduledExecutorService = scheduledExecutorService;
    this.threadFactory = threadFactory;
    this.stateManager = stateManager;
    this.compactor = compactor;
    this.memtableLoader = memtableLoader;
    this.segmentLevelMultiMapLoader = segmentLevelMultiMapLoader;
  }

  /** Initiate loading of all {@link LSMTree} resources. */
  public void load() {
    Instant startInstant = Instant.now();
    loadMemtableAndSegmentLevelMultiMap();
    logger.atInfo().log(
        "Loaded Memtable & SegmentLevel MultiMap in [%d]ms",
        Duration.between(startInstant, Instant.now()).toMillis());
    scheduleCompactor();
  }

  private void loadMemtableAndSegmentLevelMultiMap() {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure("lsm-tree-loader", threadFactory)) {
      Future<Memtable> memtable = scope.fork(memtableLoader::load);
      Future<SegmentLevelMultiMap> multiMap = scope.fork(segmentLevelMultiMapLoader::load);

      try {
        scope.join();
        scope.throwIfFailed(StorageLoadException::new);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StorageLoadException("Failed loading the LSMTree", e);
      }

      try (var ignored = stateManager.getAndLockCurrentState()) {
        stateManager.updateCurrentState(memtable.resultNow(), multiMap.resultNow());
      }
    }
  }

  private void scheduleCompactor() {
    scheduledExecutorService.scheduleWithFixedDelay(
        compactor,
        Duration.ofMinutes(0),
        Duration.ofMillis(storageConfig.getCompactorExecutionDelayMilliseconds()));
  }
}

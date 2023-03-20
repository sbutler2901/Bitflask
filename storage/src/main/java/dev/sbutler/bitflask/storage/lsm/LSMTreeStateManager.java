package dev.sbutler.bitflask.storage.lsm;

import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Manages the state of the {@link LSMTree}.
 *
 * <p>Provides exclusive and non-exclusive state via {@link CurrentState}.
 */
@Singleton
final class LSMTreeStateManager {

  private volatile Memtable memtable = null;
  private volatile SegmentLevelMultiMap segmentLevelMultiMap = null;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

  @Inject
  LSMTreeStateManager() {
  }

  LSMTreeStateManager(Memtable memtable, SegmentLevelMultiMap segmentLevelMultiMap) {
    this.memtable = memtable;
    this.segmentLevelMultiMap = segmentLevelMultiMap;
  }


  /**
   * Provides non-exclusive access to the {@link CurrentState}
   *
   * <p>NOTE: {@link CurrentState#close()} <b>must</b> be called manually or automatically using
   * try-with.
   */
  CurrentState getCurrentState() {
    lock.readLock().lock();
    checkStateInitialized();
    return new CurrentState(memtable, segmentLevelMultiMap, lock.readLock());
  }

  /**
   * Provides exclusive access to the {@link CurrentState}.
   *
   * <p>NOTE: {@link CurrentState#close()} <b>must</b> be called manually or automatically using
   * try-with.
   */
  CurrentState getAndLockCurrentState() {
    lock.writeLock().lock();
    checkStateInitialized();
    return new CurrentState(memtable, segmentLevelMultiMap, lock.writeLock());
  }

  private void checkStateInitialized() {
    if (memtable == null || segmentLevelMultiMap == null) {
      throw new StorageException("LSMTreeStateManager's state must be initialized before usage");
    }
  }

  /**
   * Atomically updates the state contained within this.
   */
  void updateCurrentState(Memtable memtable, SegmentLevelMultiMap segmentLevelMultiMap) {
    if (!lock.isWriteLockedByCurrentThread()) {
      throw new StorageException("Attempted to update CurrentState without holding lock.");
    }
    if (memtable == null || segmentLevelMultiMap == null) {
      throw new StorageException("LSMTreeStateManager's state cannot be set to null");
    }
    this.memtable = memtable;
    this.segmentLevelMultiMap = segmentLevelMultiMap;
  }

  /**
   * The current state of an {@link LSMTree}.
   *
   * <p>Access to this state was acquired with a lock which must be released using the
   * {@link #close()} method.
   */
  static class CurrentState implements AutoCloseable {

    private final Memtable memtable;
    private final SegmentLevelMultiMap segmentLevelMultiMap;
    private final Lock lock;

    private CurrentState(Memtable memtable, SegmentLevelMultiMap segmentLevelMultiMap, Lock lock) {
      this.memtable = memtable;
      this.segmentLevelMultiMap = segmentLevelMultiMap;
      this.lock = lock;
    }

    public Memtable getMemtable() {
      return memtable;
    }

    public SegmentLevelMultiMap getSegmentLevelMultiMap() {
      return segmentLevelMultiMap;
    }

    @Override
    public void close() {
      lock.unlock();
    }
  }
}

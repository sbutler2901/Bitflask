package dev.sbutler.bitflask.storage.lsm;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.memtable.Memtable;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentLevelMultiMap;
import org.junit.jupiter.api.Test;

public class LSMTreeStateManagerTest {

  private final Memtable MEMTABLE = mock(Memtable.class);
  private final SegmentLevelMultiMap MULTI_MAP = mock(SegmentLevelMultiMap.class);

  private final LSMTreeStateManager lsmTreeStateManager =
      new LSMTreeStateManager(MEMTABLE, MULTI_MAP);

  @Test
  public void getCurrentState() {
    try (var currentState = lsmTreeStateManager.getCurrentState()) {
      assertThat(currentState.getMemtable()).isEqualTo(MEMTABLE);
      assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(MULTI_MAP);
    }
  }

  @Test
  public void getCurrentState_uninitialized_throwsStorageException() {
    LSMTreeStateManager uninitializedManager = new LSMTreeStateManager();

    StorageException e =
        assertThrows(StorageException.class, uninitializedManager::getCurrentState);

    assertThat(e)
        .hasMessageThat()
        .isEqualTo("LSMTreeStateManager's state must be initialized before usage");
  }

  @Test
  public void getAndLockCurrentState() {
    try (var currentState = lsmTreeStateManager.getAndLockCurrentState()) {
      assertThat(currentState.getMemtable()).isEqualTo(MEMTABLE);
      assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(MULTI_MAP);
    }
  }

  @Test
  public void updateCurrentState_withLock() {
    Memtable newMemtable = mock(Memtable.class);
    SegmentLevelMultiMap newMultiMap = mock(SegmentLevelMultiMap.class);
    try (var ignored = lsmTreeStateManager.getAndLockCurrentState()) {
      lsmTreeStateManager.updateCurrentState(newMemtable, newMultiMap);
    }
    try (var currentState = lsmTreeStateManager.getCurrentState()) {
      assertThat(currentState.getMemtable()).isEqualTo(newMemtable);
      assertThat(currentState.getSegmentLevelMultiMap()).isEqualTo(newMultiMap);
    }
  }

  @Test
  public void updateCurrentState_withLock_memtableNull_throwsStorageException() {
    SegmentLevelMultiMap newMultiMap = mock(SegmentLevelMultiMap.class);

    StorageException exception;
    try (var ignored = lsmTreeStateManager.getAndLockCurrentState()) {
      exception =
          assertThrows(
              StorageException.class,
              () -> lsmTreeStateManager.updateCurrentState(null, newMultiMap));
    }

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("LSMTreeStateManager's state cannot be set to null");
  }

  @Test
  public void updateCurrentState_withLock_egmentLevelMultiMapNull_throwsStorageException() {
    Memtable newMemtable = mock(Memtable.class);

    StorageException exception;
    try (var ignored = lsmTreeStateManager.getAndLockCurrentState()) {
      exception =
          assertThrows(
              StorageException.class,
              () -> lsmTreeStateManager.updateCurrentState(newMemtable, null));
    }

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("LSMTreeStateManager's state cannot be set to null");
  }

  @Test
  public void updateCurrentState_withoutLock_throwsStorageException() {
    StorageException e =
        assertThrows(
            StorageException.class,
            () -> lsmTreeStateManager.updateCurrentState(MEMTABLE, MULTI_MAP));

    assertThat(e)
        .hasMessageThat()
        .isEqualTo("Attempted to update CurrentState without holding lock.");
  }
}

package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link RaftVolatileState}. */
public class RaftVolatileStateTest {

  private final RaftVolatileState raftVolatileState = new RaftVolatileState();

  @Test
  public void initialize() {
    assertThat(raftVolatileState.getHighestCommittedEntryIndex()).isEqualTo(0);
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(0);

    raftVolatileState.initialize(1, 1);

    assertThat(raftVolatileState.getHighestCommittedEntryIndex()).isEqualTo(1);
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(1);
  }

  @Test
  public void increaseHighestCommittedEntryIndexTo() {
    assertThat(raftVolatileState.getHighestCommittedEntryIndex()).isEqualTo(0);

    raftVolatileState.increaseHighestCommittedEntryIndexTo(1);

    assertThat(raftVolatileState.getHighestCommittedEntryIndex()).isEqualTo(1);
  }

  @Test
  public void
      increaseHighestCommittedEntryIndexTo_lessThanCurrent_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> raftVolatileState.increaseHighestCommittedEntryIndexTo(-1));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Attempting to set committed entry index lower than current value. [current=0, new=-1].");
  }

  @Test
  public void increaseHighestAppliedEntryIndexTo() {
    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(0);
    raftVolatileState.increaseHighestCommittedEntryIndexTo(1);

    raftVolatileState.increaseHighestAppliedEntryIndexTo(1);

    assertThat(raftVolatileState.getHighestAppliedEntryIndex()).isEqualTo(1);
  }

  @Test
  public void increaseHighestAppliedEntryIndexTo_lessThanCurrent_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> raftVolatileState.increaseHighestAppliedEntryIndexTo(-1));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Attempting to set applied entry index lower than current value. [current=0, new=-1].");
  }

  @Test
  public void
      increaseHighestAppliedEntryIndexTo_greaterThanCommitIndex_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> raftVolatileState.increaseHighestAppliedEntryIndexTo(1));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "Attempting to set applied entry index [1] higher than committed entry index [0].");
  }

  @Test
  public void setLeaderServerId() {
    assertThat(raftVolatileState.getLeaderServerId()).isEmpty();
    RaftServerId serverId = new RaftServerId("test");

    raftVolatileState.setLeaderServerId(serverId);

    assertThat(raftVolatileState.getLeaderServerId()).hasValue(serverId);
  }

  @Test
  public void setLeaderServerId_setToNull_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class, () -> raftVolatileState.setLeaderServerId(null));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Leader's server id cannot be set to null. Use clearLeaderServerId().");
  }

  @Test
  public void clearLeaderServerId() {
    RaftServerId serverId = new RaftServerId("test");
    raftVolatileState.setLeaderServerId(serverId);
    assertThat(raftVolatileState.getLeaderServerId()).hasValue(serverId);

    raftVolatileState.clearLeaderServerId();

    assertThat(raftVolatileState.getLeaderServerId()).isEmpty();
  }
}

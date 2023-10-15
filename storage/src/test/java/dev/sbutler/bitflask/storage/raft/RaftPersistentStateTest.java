package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.config.ServerConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RaftPersistentState}. */
public class RaftPersistentStateTest {

  private static final RaftServerId THIS_SERVER_ID = new RaftServerId("this-server");
  private static final RaftConfiguration RAFT_CONFIGURATION =
      new RaftConfiguration(
          THIS_SERVER_ID,
          ImmutableMap.of(THIS_SERVER_ID, ServerConfig.ServerInfo.getDefaultInstance()),
          new RaftTimerInterval(0, 100));

  private final RaftPersistentState raftPersistentState =
      new RaftPersistentState(RAFT_CONFIGURATION);

  @Test
  public void initialize() {
    assertThat(raftPersistentState.getCurrentTerm()).isEqualTo(0);
    assertThat(raftPersistentState.getVotedForCandidateId()).isEmpty();

    raftPersistentState.initialize(1, Optional.empty());
    assertThat(raftPersistentState.getCurrentTerm()).isEqualTo(1);
    assertThat(raftPersistentState.getVotedForCandidateId()).isEmpty();

    raftPersistentState.initialize(2, Optional.of(THIS_SERVER_ID));
    assertThat(raftPersistentState.getCurrentTerm()).isEqualTo(2);
    assertThat(raftPersistentState.getVotedForCandidateId()).hasValue(THIS_SERVER_ID);
  }

  @Test
  public void setCurrentTermAndResetVote() {
    raftPersistentState.setCurrentTermAndResetVote(1);

    assertThat(raftPersistentState.getCurrentTerm()).isEqualTo(1);
    assertThat(raftPersistentState.getVotedForCandidateId()).isEmpty();
  }

  @Test
  public void setCurrentTermAndResetVote_newTermLowerThanCurrent_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> raftPersistentState.setCurrentTermAndResetVote(-1));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Current term cannot be decreased. [currentTerm=0, newCurrentTerm=-1].");
  }

  @Test
  public void incrementTermAndVoteForSelf() {
    raftPersistentState.incrementTermAndVoteForSelf();

    assertThat(raftPersistentState.getCurrentTerm()).isEqualTo(1);
    assertThat(raftPersistentState.getVotedForCandidateId()).hasValue(THIS_SERVER_ID);
  }

  @Test
  public void setVotedForCandidateId() {
    raftPersistentState.setVotedForCandidateId(THIS_SERVER_ID);

    assertThat(raftPersistentState.getCurrentTerm()).isEqualTo(0);
    assertThat(raftPersistentState.getVotedForCandidateId()).hasValue(THIS_SERVER_ID);
  }

  @Test
  public void setVotedForCandidateId_unknownCandidateId_throwsIllegalArgumentException() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> raftPersistentState.setVotedForCandidateId(new RaftServerId("unknown-server")));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Attempting to vote for unknown server [unknown-server] for term [0].");
  }

  @Test
  public void setVotedForCandidateId_alreadyVoted_throwsIllegalArgumentException() {
    raftPersistentState.setVotedForCandidateId(THIS_SERVER_ID);
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> raftPersistentState.setVotedForCandidateId(THIS_SERVER_ID));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo("Already voted for a candidate this term [0].");
  }
}

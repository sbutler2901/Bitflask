package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.config.ServerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RaftLeaderStateTest {

  private static final RaftServerId serverId = new RaftServerId("server");
  private static final RaftLog.LogEntryDetails lastLogEntryDetails =
      new RaftLog.LogEntryDetails(0, 0);

  private final RaftConfiguration configuration = mock(RaftConfiguration.class);
  private final RaftLog raftLog = mock(RaftLog.class);

  private RaftLeaderState leaderState;

  @BeforeEach
  public void beforeEach() {
    when(raftLog.getLastLogEntryDetails()).thenReturn(lastLogEntryDetails);
    when(configuration.getOtherServersInCluster())
        .thenReturn(ImmutableMap.of(serverId, ServerConfig.ServerInfo.getDefaultInstance()));
    leaderState = new RaftLeaderState(configuration, raftLog);
  }

  @Test
  public void getFollowerNextIndex_initialState() {
    assertThat(leaderState.getFollowerNextIndex(serverId))
        .isEqualTo(1 + lastLogEntryDetails.index());
  }

  @Test
  public void getFollowerNextIndex_invalidServerId() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> leaderState.getFollowerNextIndex(new RaftServerId("other_server")));
    assertThat(exception).hasMessageThat().contains("other_server");
  }

  @Test
  public void getFollowerMatchIndex_initialState() {
    assertThat(leaderState.getFollowerMatchIndex(serverId)).isEqualTo(0);
  }

  @Test
  public void getFollowerMatchIndex_invalidServerId() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> leaderState.getFollowerMatchIndex(new RaftServerId("other_server")));
    assertThat(exception).hasMessageThat().contains("other_server");
  }

  @Test
  public void increaseFollowerNextIndex_submissionGreater() {
    assertThat(leaderState.getFollowerNextIndex(serverId))
        .isEqualTo(1 + lastLogEntryDetails.index());
    leaderState.increaseFollowerNextIndex(
        new RaftLeaderProcessor.AppendEntriesSubmission(
            serverId,
            /* followerNextIndex= */ 0,
            /* lastEntryIndex= */ Integer.MAX_VALUE,
            AppendEntriesRequest.getDefaultInstance(),
            immediateFuture(AppendEntriesResponse.getDefaultInstance())));

    assertThat(leaderState.getFollowerNextIndex(serverId)).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void increaseFollowerNextIndex_prevGreater() {
    assertThat(leaderState.getFollowerNextIndex(serverId))
        .isEqualTo(1 + lastLogEntryDetails.index());
    leaderState.increaseFollowerNextIndex(
        new RaftLeaderProcessor.AppendEntriesSubmission(
            serverId,
            /* followerNextIndex= */ 0,
            /* lastEntryIndex= */ Integer.MIN_VALUE,
            AppendEntriesRequest.getDefaultInstance(),
            immediateFuture(AppendEntriesResponse.getDefaultInstance())));

    assertThat(leaderState.getFollowerNextIndex(serverId))
        .isEqualTo(1 + lastLogEntryDetails.index());
  }

  @Test
  public void decreaseFollowerNextIndex_minimumValue_throwsIllegalStateException() {
    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () ->
                leaderState.decreaseFollowerNextIndex(
                    new RaftLeaderProcessor.AppendEntriesSubmission(
                        serverId,
                        /* followerNextIndex= */ 0,
                        /* lastEntryIndex= */ 0,
                        AppendEntriesRequest.getDefaultInstance(),
                        immediateFuture(AppendEntriesResponse.getDefaultInstance()))));

    assertThat(exception).hasMessageThat().contains(serverId.toString());
    assertThat(exception).hasMessageThat().contains("below 0");
  }

  @Test
  public void increaseFollowerMatchIndex_submissionGreater() {
    assertThat(leaderState.getFollowerMatchIndex(serverId)).isEqualTo(0);
    leaderState.increaseFollowerMatchIndex(
        new RaftLeaderProcessor.AppendEntriesSubmission(
            serverId,
            /* followerNextIndex= */ 0,
            /* lastEntryIndex= */ Integer.MAX_VALUE,
            AppendEntriesRequest.getDefaultInstance(),
            immediateFuture(AppendEntriesResponse.getDefaultInstance())));

    assertThat(leaderState.getFollowerMatchIndex(serverId)).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void increaseFollowerMatchIndex_prevGreater() {
    assertThat(leaderState.getFollowerMatchIndex(serverId)).isEqualTo(0);
    leaderState.increaseFollowerMatchIndex(
        new RaftLeaderProcessor.AppendEntriesSubmission(
            serverId,
            /* followerNextIndex= */ 0,
            /* lastEntryIndex= */ Integer.MIN_VALUE,
            AppendEntriesRequest.getDefaultInstance(),
            immediateFuture(AppendEntriesResponse.getDefaultInstance())));

    assertThat(leaderState.getFollowerMatchIndex(serverId)).isEqualTo(0);
  }
}

package dev.sbutler.bitflask.storage.raft;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static dev.sbutler.bitflask.storage.raft.RaftCandidateProcessor.RequestVotesSubmission;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.google.common.collect.ImmutableList;
import jakarta.inject.Inject;

/** Utility class for handling rpc calls used by the {@link RaftCandidateProcessor}. */
final class RaftCandidateRpcClient {

  private final RaftConfiguration raftConfiguration;
  private final RaftRpcChannelManager rpcChannelManager;
  private final RaftPersistentState raftPersistentState;

  @Inject
  RaftCandidateRpcClient(
      RaftConfiguration raftConfiguration,
      RaftRpcChannelManager rpcChannelManager,
      RaftPersistentState raftPersistentState) {
    this.raftConfiguration = raftConfiguration;
    this.rpcChannelManager = rpcChannelManager;
    this.raftPersistentState = raftPersistentState;
  }

  interface Factory {
    RaftCandidateRpcClient create();
  }

  ImmutableList<RequestVotesSubmission> broadcastRequestVotes(int electionTimeout) {
    RequestVoteRequest request = getRequest();
    return raftConfiguration.getOtherServersInCluster().keySet().stream()
        .map(
            serverId ->
                new RequestVotesSubmission(
                    serverId,
                    request,
                    rpcChannelManager
                        .getStubForServer(serverId)
                        .withDeadlineAfter(electionTimeout, MILLISECONDS)
                        .requestVote(request)))
        .collect(toImmutableList());
  }

  private RequestVoteRequest getRequest() {
    RaftLog.LogEntryDetails lastLogEntryDetails =
        raftPersistentState.getRaftLog().getLastLogEntryDetails();
    return RequestVoteRequest.newBuilder()
        .setCandidateId(raftConfiguration.thisRaftServerId().id())
        .setTerm(raftPersistentState.getCurrentTerm())
        .setLastLogIndex(lastLogEntryDetails.index())
        .setLastLogTerm(lastLogEntryDetails.term())
        .build();
  }
}

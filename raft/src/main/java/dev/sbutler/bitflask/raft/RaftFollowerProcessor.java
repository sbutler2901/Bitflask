package dev.sbutler.bitflask.raft;

final class RaftFollowerProcessor implements RaftModeProcessor {

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }

  @Override
  public void handleElectionTimeout() {}
}

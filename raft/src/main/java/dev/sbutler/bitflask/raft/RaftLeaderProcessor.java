package dev.sbutler.bitflask.raft;

final class RaftLeaderProcessor implements RaftStateProcessor {

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }
}

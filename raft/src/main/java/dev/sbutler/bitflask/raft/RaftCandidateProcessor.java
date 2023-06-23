package dev.sbutler.bitflask.raft;

final class RaftCandidateProcessor implements RaftModeProcessor, Runnable {

  @Override
  public RequestVoteResponse processRequestVoteRequest(RequestVoteRequest request) {
    return RequestVoteResponse.getDefaultInstance();
  }

  @Override
  public AppendEntriesResponse processAppendEntriesRequest(AppendEntriesRequest request) {
    return AppendEntriesResponse.getDefaultInstance();
  }

  @Override
  public void run() {
    startNewElection();
  }

  void handleElectionTimeout() {
    cancelCurrentElection();
    startNewElection();
  }

  private void startNewElection() {
    // increment current term
    // vote for self
    // reset election timer
    // Send RequestVote RPCs to all other servers
  }

  private void cancelCurrentElection() {}
}

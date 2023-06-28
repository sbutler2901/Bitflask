package dev.sbutler.bitflask.raft;

/** Implemented by classes that can handle election timeouts. */
interface RaftElectionTimeoutHandler {
  /** Called when an election timeout occurs. */
  void handleElectionTimeout();
}

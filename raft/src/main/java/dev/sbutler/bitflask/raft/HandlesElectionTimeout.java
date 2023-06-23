package dev.sbutler.bitflask.raft;

/** Implemented by classes that can handle election timeouts. */
interface HandlesElectionTimeout {
  /** Called when an election timeout occurs. */
  void handleElectionTimeout();
}

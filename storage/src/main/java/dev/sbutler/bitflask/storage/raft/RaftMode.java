package dev.sbutler.bitflask.storage.raft;

/** Represents the modes a Raft server can be in. */
public enum RaftMode {
  /** The server is only listening from incoming RPCs. */
  FOLLOWER,
  /** The server is attempting to become the leader of the cluster. */
  CANDIDATE,
  /** The server is receiving client requests and replicating across the cluster. */
  LEADER
}

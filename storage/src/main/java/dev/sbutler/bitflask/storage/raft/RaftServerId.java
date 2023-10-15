package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;

/** The ID of a server in a raft cluster. */
public record RaftServerId(String id) {

  public RaftServerId {
    Preconditions.checkArgument(!id.isBlank(), "A RaftServerId cannot be blank.");
  }
}

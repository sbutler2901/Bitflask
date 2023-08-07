package dev.sbutler.bitflask.storage.raft;

/** Commands that can be accepted and replicated. */
public sealed interface RaftCommand {

  record SetCommand(String key, String value) implements RaftCommand {}

  record DeleteCommand(String key) implements RaftCommand {}
}

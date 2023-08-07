package dev.sbutler.bitflask.storage.raft;

/** The information for a particular Raft gRPC server. */
public record RaftServerInfo(RaftServerId id, String host, int port) {}

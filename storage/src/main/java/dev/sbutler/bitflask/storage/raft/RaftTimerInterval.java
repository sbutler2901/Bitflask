package dev.sbutler.bitflask.storage.raft;

/** The time interval for the {@link RaftElectionTimer} to use when picking a timeout randomly. */
public record RaftTimerInterval(int minimumMilliSeconds, int maximumMilliseconds) {}

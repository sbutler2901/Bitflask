package dev.sbutler.bitflask.storage.raft;

/** The time interval for raft election timeouts. */
public record RaftTimerInterval(int minimumMilliSeconds, int maximumMilliseconds) {}

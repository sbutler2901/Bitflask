package dev.sbutler.bitflask.raft;

/** The time interval for the {@link RaftElectionTimer} to use when picking a timeout randomly. */
record RaftTimerInterval(long minimumMilliSeconds, long maximumMilliseconds) {}

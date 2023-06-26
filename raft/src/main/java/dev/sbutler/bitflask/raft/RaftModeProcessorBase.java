package dev.sbutler.bitflask.raft;

/**
 * Base implementation of {@link RaftModeProcessor} providing some generic implementations of
 * required methods.
 */
abstract sealed class RaftModeProcessorBase implements RaftModeProcessor
    permits RaftFollowerProcessor, RaftCandidateProcessor, RaftLeaderProcessor {}

package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Handles initializing raft at start up. */
public final class RaftLoader {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftPersistentState raftPersistentState;
  private final RaftVolatileState raftVolatileState;

  @Inject
  RaftLoader(RaftPersistentState raftPersistentState, RaftVolatileState raftVolatileState) {
    this.raftPersistentState = raftPersistentState;
    this.raftVolatileState = raftVolatileState;
  }

  public void load() {
    Instant startInstant = Instant.now();
    // TODO: implement proper initialization
    raftPersistentState.initialize(0, Optional.empty());
    raftVolatileState.initialize(0, 0);
    logger.atInfo().log(
        "Loaded Storage in [%d]ms", Duration.between(startInstant, Instant.now()).toMillis());
  }
}

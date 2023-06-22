package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/** The Raft timer used for managing election timeouts. */
final class RaftElectionTimer {

  private final RaftTimerInterval raftTimerInterval;
  private final RaftStateManager raftStateManager;
  private final Timer timer = new Timer("raft-election-timer", true);

  private volatile RaftTimerTask currentTimerTask;

  @Inject
  RaftElectionTimer(
      RaftClusterConfiguration raftClusterConfiguration, RaftStateManager raftStateManager) {
    this.raftTimerInterval = raftClusterConfiguration.raftTimerInterval();
    this.raftStateManager = raftStateManager;
    this.currentTimerTask = new RaftTimerTask();
  }

  /** Cancels the current timer and starts a new one. */
  void restart() {
    cancel();
    currentTimerTask = new RaftTimerTask();
    timer.schedule(
        currentTimerTask,
        ThreadLocalRandom.current()
            .nextLong(
                raftTimerInterval.minimumMilliSeconds(),
                1 + raftTimerInterval.maximumMilliseconds()));
  }

  /** Cancels the current timer without rescheduling. */
  void cancel() {
    if (currentTimerTask != null) {
      currentTimerTask.cancel();
      currentTimerTask = null;
    }
  }

  /** The task to execute once the {@link RaftElectionTimer} has expired. */
  static final class RaftTimerTask extends TimerTask {

    @Override
    public void run() {
      // TODO: decide how to handle state changes.
    }
  }
}

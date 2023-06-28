package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/** The Raft timer used for managing election timeouts. */
@Singleton
final class RaftElectionTimer {

  private final RaftTimerInterval raftTimerInterval;
  private final Timer timer = new Timer("raft-election-timer", true);

  private volatile RaftElectionTimeoutHandler timeoutHandler;
  private volatile TimerTask currentTimerTask;

  @Inject
  RaftElectionTimer(RaftClusterConfiguration raftClusterConfiguration) {
    this.raftTimerInterval = raftClusterConfiguration.raftTimerInterval();
  }

  /**
   * Sets the {@link RaftElectionTimeoutHandler} implementing object that will be called on election
   * timeout.
   */
  void setTimeoutHandler(RaftElectionTimeoutHandler timeoutHandler) {
    this.timeoutHandler = timeoutHandler;
  }

  /** Cancels the current timer and starts a new one. */
  void restart() {
    cancel();

    currentTimerTask =
        new TimerTask() {
          @Override
          public void run() {
            timeoutHandler.handleElectionTimeout();
          }
        };

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
}

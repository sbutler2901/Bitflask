package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ThreadLocalRandom;

/** The Raft timer used for managing election timeouts. */
@Singleton
final class RaftElectionTimer {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftTimerInterval raftTimerInterval;
  private final Provider<RaftModeManager> raftModeManager;
  private final Timer timer = new Timer("raft-election-timer", true);

  private volatile TimerTask currentTimerTask;

  @Inject
  RaftElectionTimer(
      RaftConfiguration raftConfiguration, Provider<RaftModeManager> raftModeManager) {
    this.raftTimerInterval = raftConfiguration.raftTimerInterval();
    this.raftModeManager = raftModeManager;
  }

  /** Cancels the current timer and starts a new one. */
  void restart() {
    cancel();

    currentTimerTask =
        new TimerTask() {
          @Override
          public void run() {
            raftModeManager.get().handleElectionTimeout();
          }
        };

    int timerDelay =
        ThreadLocalRandom.current()
            .nextInt(
                raftTimerInterval.minimumMilliSeconds(),
                1 + raftTimerInterval.maximumMilliseconds());
    timer.schedule(currentTimerTask, timerDelay);
    logger.atFine().log("Restarted election timer with [%d] delay.", timerDelay);
  }

  /** Cancels the current timer without rescheduling. */
  void cancel() {
    if (currentTimerTask != null) {
      currentTimerTask.cancel();
      currentTimerTask = null;
      logger.atFine().log("Canceled election timer.");
    }
  }
}

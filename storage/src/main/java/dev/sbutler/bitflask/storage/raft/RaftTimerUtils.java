package dev.sbutler.bitflask.storage.raft;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class RaftTimerUtils {

  /**
   * Generates a random delay within the bounds (inclusive) of the {@link RaftTimerInterval} in
   * milliseconds.
   */
  static int getRandomDelayMillis(RaftTimerInterval raftTimerInterval) {
    return ThreadLocalRandom.current()
        .nextInt(
            raftTimerInterval.minimumMilliSeconds(), 1 + raftTimerInterval.maximumMilliseconds());
  }

  /** Provides an {@link Instant} {@code delayMillis} from now. */
  static Instant getExpirationFromNow(long delayMillis) {
    return Instant.now().plusMillis(delayMillis);
  }

  /**
   * Waits until the system clock reaches the {@code waitExpiration}, {@code exitEarly} returns
   * true, or the thread is interrupted.
   */
  static void waitUntilExpiration(Instant waitExpiration, Supplier<Boolean> exitEarly) {
    while (!exitEarly.get()
        && Instant.now().isBefore(waitExpiration)
        && !Thread.currentThread().isInterrupted()) {
      Thread.onSpinWait();
    }
  }

  /** Waits until the system clock reaches {@code expiration} or the thread is interrupted. */
  static void waitWithDynamicExpiration(Supplier<Instant> expiration) {
    while (Instant.now().isBefore(expiration.get()) && !Thread.currentThread().isInterrupted()) {
      Thread.onSpinWait();
    }
  }

  private RaftTimerUtils() {}
}

package dev.sbutler.bitflask.common.concurrency;

import static com.google.mu.util.stream.GuavaCollectors.toImmutableMap;

import com.google.common.collect.ImmutableMap;
import com.google.mu.util.stream.BiStream;
import java.util.concurrent.Future;
import java.util.concurrent.Future.State;

/**
 * Utility methods for code that utilizes {@link jdk.incubator.concurrent.StructuredTaskScope}
 */
public class StructuredTaskScopeUtils {

  public static <K, V> ImmutableMap<K, V> getSuccessfulFutureValuesFromMap(
      ImmutableMap<K, Future<V>> futureMap) {
    return BiStream.from(futureMap)
        .filterValues(f -> f.state() == State.SUCCESS)
        .mapValues(Future::resultNow)
        .collect(toImmutableMap());
  }

  public static <K, V> ImmutableMap<K, Throwable> getFailedFutureThrowablesFromMap(
      ImmutableMap<K, Future<V>> futureMap) {
    return BiStream.from(futureMap)
        .filterValues(f -> f.state() != State.SUCCESS)
        .mapValues(Future::exceptionNow)
        .collect(toImmutableMap());
  }

  private StructuredTaskScopeUtils() {

  }
}

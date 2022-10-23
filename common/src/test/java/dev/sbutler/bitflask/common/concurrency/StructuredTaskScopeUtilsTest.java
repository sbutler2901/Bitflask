package dev.sbutler.bitflask.common.concurrency;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;
import java.io.IOException;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

public class StructuredTaskScopeUtilsTest {

  @Test
  public void getSuccessfulFutureValuesFromMap() {
    // Arrange
    SettableFuture<String> successFuture = SettableFuture.create();
    successFuture.set("successFuture");
    SettableFuture<String> failedFuture = SettableFuture.create();
    failedFuture.setException(new IOException("test"));
    ImmutableMap<String, Future<String>> futureMap =
        ImmutableMap.of("success", successFuture, "failed", failedFuture);
    // Act
    ImmutableMap<String, String> successfulMap =
        StructuredTaskScopeUtils.getSuccessfulFutureValuesFromMap(futureMap);
    // Assert
    assertThat(successfulMap).hasSize(1);
    assertThat(successfulMap).containsEntry("success", "successFuture");
  }

  @Test
  public void getFailedFutureThrowablesFromMap() {
    // Arrange
    SettableFuture<String> successFuture = SettableFuture.create();
    successFuture.set("successFuture");
    SettableFuture<String> failedFuture = SettableFuture.create();
    IOException exception = new IOException("test");
    failedFuture.setException(exception);
    ImmutableMap<String, Future<String>> futureMap =
        ImmutableMap.of("success", successFuture, "failed", failedFuture);
    // Act
    ImmutableMap<String, Throwable> failedMap =
        StructuredTaskScopeUtils.getFailedFutureThrowablesFromMap(futureMap);
    // Assert
    assertThat(failedMap).hasSize(1);
    assertThat(failedMap).containsEntry("failed", exception);
  }
}

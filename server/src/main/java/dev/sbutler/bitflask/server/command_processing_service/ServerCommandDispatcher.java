package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import dev.sbutler.bitflask.server.configuration.ServerCommandDispatcherCapacity;
import dev.sbutler.bitflask.storage.DispatcherClosedException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ServerCommandDispatcher {

  private final BlockingDeque<ServerCommandSubmission> submissions;
  private volatile boolean isOpen = true;

  @Inject
  ServerCommandDispatcher(@ServerCommandDispatcherCapacity int capacity) {
    submissions = new LinkedBlockingDeque<>(capacity);
  }

  /**
   * Submits the provide command to the dispatcher, waiting if necessary for space to be available
   *
   * <p>The future will fail with a {@link DispatcherClosedException} if the dispatcher is no
   * longer accepting new submissions.
   *
   * @return a future resolving with the ServerResponse, or an InterruptedException if interrupted
   * prior to submission
   */
  public ListenableFuture<ServerResponse> put(ServerCommand serverCommand) {
    SettableFuture<ServerResponse> response = SettableFuture.create();
    if (!isOpen) {
      response.setException(new DispatcherClosedException());
      return response;
    }

    try {
      submissions.putLast(new ServerCommandSubmission(serverCommand, response));
    } catch (InterruptedException e) {
      response.setException(e);
    }
    return response;
  }

  /**
   * Submits the provided command to the dispatcher if space is available.
   *
   * <p>The future will fail with an {@link IllegalStateException} if the submission could not be
   * immediately submitted, or a {@link DispatcherClosedException} if the dispatcher is no longer
   * accepting new submissions.
   *
   * @return a future resolving with the ServerResponse, or an IllegalStateException if space was
   * not available
   */
  public ListenableFuture<ServerResponse> offer(ServerCommand serverCommand) {
    SettableFuture<ServerResponse> response = SettableFuture.create();
    if (!isOpen) {
      response.setException(new DispatcherClosedException());
      return response;
    }
    if (!submissions.offerLast(new ServerCommandSubmission(serverCommand, response))) {
      response.setException(new IllegalStateException("Dispatcher queue is full"));
    }
    return response;
  }

  ServerCommandSubmission poll(long timeout, TimeUnit unit) throws InterruptedException {
    return submissions.pollFirst(timeout, unit);
  }

  void closeAndDrain() {
    isOpen = false;
    while (!submissions.isEmpty()) {
      var future = submissions.pollFirst();
      if (future != null) {
        future.response().setException(new DispatcherClosedException());
      }
    }
  }
}

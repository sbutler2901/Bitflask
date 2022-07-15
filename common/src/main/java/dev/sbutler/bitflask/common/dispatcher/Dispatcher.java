package dev.sbutler.bitflask.common.dispatcher;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/**
 * Acts as a dispatcher of objects to a consumer who will asynchronously provide a response
 *
 * @param <C> an object to be consumed by a consumer of the dispatcher
 * @param <R> a response to be provided by a dispatcher's consumer
 */
public abstract class Dispatcher<C, R> {

  private final BlockingDeque<DispatcherSubmission<C, R>> submissions;
  private volatile boolean isOpen = true;

  /**
   * Instantiates the dispatcher with the provided submission capacity
   */
  protected Dispatcher(int capacity) {
    submissions = new LinkedBlockingDeque<>(capacity);
  }

  /**
   * Submits the provided {@link C} to the dispatcher, waiting if necessary for space to be
   * available
   *
   * <p>The future will fail with a {@link DispatcherClosedException} if the dispatcher is no
   * longer accepting new submissions.
   *
   * @return a future resolving with the {@link R}, or an InterruptedException if interrupted prior
   * to submission
   */
  public ListenableFuture<R> put(C command) {
    SettableFuture<R> responseFuture = SettableFuture.create();
    if (!isOpen) {
      responseFuture.setException(new DispatcherClosedException());
      return responseFuture;
    }

    try {
      submissions.putLast(new DispatcherSubmission<>(command, responseFuture));
    } catch (InterruptedException e) {
      responseFuture.setException(e);
    }
    return responseFuture;
  }

  /**
   * Submits the provided {@link C} to the dispatcher if space is available.
   *
   * <p>The future will fail with an {@link IllegalStateException} if the submission could not be
   * immediately submitted, or a {@link DispatcherClosedException} if the dispatcher is no longer
   * accepting new submissions.
   *
   * @return a future resolving with the {@link R}, or an IllegalStateException if space was not
   * available
   */
  public ListenableFuture<R> offer(C command) {
    SettableFuture<R> responseFuture = SettableFuture.create();
    if (!isOpen) {
      responseFuture.setException(new DispatcherClosedException());
      return responseFuture;
    }

    if (!submissions.offerLast(new DispatcherSubmission<>(command, responseFuture))) {
      responseFuture.setException(new IllegalStateException("Dispatcher queue is full"));
    }
    return responseFuture;
  }

  /**
   * Retrieves the next submission waiting for the provided time if empty
   */
  public DispatcherSubmission<C, R> poll(long timeout, TimeUnit unit) throws InterruptedException {
    return submissions.pollFirst(timeout, unit);
  }

  /**
   * Closes the dispatcher and completes all outstanding submissions exceptionally with
   * {@link DispatcherClosedException}
   */
  public void closeAndDrain() {
    isOpen = false;
    while (!submissions.isEmpty()) {
      DispatcherSubmission<C, R> submission = submissions.pollFirst();
      if (submission != null) {
        submission.responseFuture().setException(new DispatcherClosedException());
      }
    }
  }
}

package dev.sbutler.bitflask.common.dispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DispatcherTest {

  TestDispatcher dispatcher;

  @BeforeEach
  void beforeEach() {
    dispatcher = new TestDispatcher(1);
  }

  @Test
  void put() {
    // Arrange / Act
    ListenableFuture<Integer> responseFuture = dispatcher.put(0);
    // Assert
    assertFalse(responseFuture.isDone());
  }

  @Test
  void put_blocking() throws Exception {
    // Arrange
    dispatcher.put(0);
    AtomicReference<ListenableFuture<Integer>> responseFuture1 = new AtomicReference<>();
    Thread blockingThread = new Thread(() -> responseFuture1.set(dispatcher.put(1)));
    blockingThread.start();
    // Act
    blockingThread.interrupt();
    blockingThread.join();
    // Assert
    assertTrue(responseFuture1.get().isDone());
    try {
      responseFuture1.get().get();
    } catch (ExecutionException e) {
      assertInstanceOf(InterruptedException.class, e.getCause());
    }
  }

  @Test
  void put_closed() throws Exception {
    // Arrange
    dispatcher.closeAndDrain();
    // Act
    ListenableFuture<Integer> responseFuture = dispatcher.put(0);
    // Assert
    try {
      responseFuture.get();
    } catch (ExecutionException e) {
      assertInstanceOf(DispatcherClosedException.class, e.getCause());
    }
  }

  @Test
  void offer() {
    // Arrange / Act
    ListenableFuture<Integer> responseFuture = dispatcher.offer(0);
    // Assert
    assertFalse(responseFuture.isDone());
  }

  @Test
  void offer_dispatcherFull() throws Exception {
    // Arrange / Act
    ListenableFuture<Integer> responseFuture0 = dispatcher.offer(0);
    ListenableFuture<Integer> responseFuture1 = dispatcher.offer(1);
    // Assert
    assertFalse(responseFuture0.isDone());
    assertTrue(responseFuture1.isDone());
    try {
      responseFuture1.get();
    } catch (ExecutionException e) {
      assertInstanceOf(IllegalStateException.class, e.getCause());
    }
  }

  @Test
  void offer_closed() throws Exception {
    // Arrange
    dispatcher.closeAndDrain();
    // Act
    ListenableFuture<Integer> responseFuture = dispatcher.offer(0);
    // Assert
    try {
      responseFuture.get();
    } catch (ExecutionException e) {
      assertInstanceOf(DispatcherClosedException.class, e.getCause());
    }
  }

  @Test
  void poll() throws Exception {
    // Arrange
    ListenableFuture<Integer> responseFuture = dispatcher.put(1);
    // Act
    Optional<DispatcherSubmission<Integer, Integer>> submission = dispatcher.poll(1,
        TimeUnit.SECONDS);
    submission.get().responseFuture().set(2);
    // Assert
    assertTrue(responseFuture.isDone());
    assertEquals(2, responseFuture.get());
  }

  @Test
  void closeAndDrain() throws Exception {
    // Arrange
    ListenableFuture<Integer> responseFuture = dispatcher.offer(0);
    // Act
    dispatcher.closeAndDrain();
    // Assert
    assertTrue(responseFuture.isDone());
    try {
      responseFuture.get();
    } catch (ExecutionException e) {
      assertInstanceOf(DispatcherClosedException.class, e.getCause());
    }
  }

  static class TestDispatcher extends Dispatcher<Integer, Integer> {

    protected TestDispatcher(int capacity) {
      super(capacity);
    }
  }

}

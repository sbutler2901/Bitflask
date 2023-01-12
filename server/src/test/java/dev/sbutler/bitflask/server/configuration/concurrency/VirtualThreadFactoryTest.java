package dev.sbutler.bitflask.server.configuration.concurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.Thread.State;
import org.junit.jupiter.api.Test;

public class VirtualThreadFactoryTest {

  private final VirtualThreadFactory virtualThreadFactory = new VirtualThreadFactory();

  @Test
  void newThread() {
    Runnable runnable = mock(Runnable.class);
    Thread thread = virtualThreadFactory.newThread(runnable);
    assertTrue(thread.isVirtual());
    assertEquals(thread.getState(), State.NEW);
    assertTrue(thread.getName().contains(VirtualThreadFactory.NETWORK_SERVICE_THREAD_NAME));
  }

}

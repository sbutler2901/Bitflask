package dev.sbutler.bitflask.server.configuration.concurrency;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class ServerThreadFactoryTest {

  private final ServerThreadFactory serverThreadFactory = new ServerThreadFactory();

  @Test
  void newThread() {
    Runnable runnable = mock(Runnable.class);
    Thread thread = serverThreadFactory.newThread(runnable);
    assertTrue(thread.getName().contains("server-pool"));
  }

}

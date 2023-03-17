package dev.sbutler.bitflask.common.concurrency;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import java.lang.Thread.State;
import org.junit.jupiter.api.Test;

public class VirtualThreadFactoryTest {


  @Test
  void newThread_defaultThreadNamePrefix() {
    VirtualThreadFactory virtualThreadFactory = new VirtualThreadFactory();
    Runnable runnable = mock(Runnable.class);

    Thread thread = virtualThreadFactory.newThread(runnable);

    assertThat(thread.isVirtual()).isTrue();
    assertThat(thread.getState()).isEqualTo(State.NEW);
    assertThat(thread.getName()).startsWith(VirtualThreadFactory.DEFAULT_THREAD_NAME);
  }

  @Test
  void newThread_providedThreadNamePrefix() {
    VirtualThreadFactory virtualThreadFactory = new VirtualThreadFactory("test-prefix-");
    Runnable runnable = mock(Runnable.class);

    Thread thread = virtualThreadFactory.newThread(runnable);

    assertThat(thread.isVirtual()).isTrue();
    assertThat(thread.getState()).isEqualTo(State.NEW);
    assertThat(thread.getName()).startsWith("test-prefix-");
  }
}

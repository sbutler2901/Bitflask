package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class StorageImplTest {

  @InjectMocks
  StorageImpl storage;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentManager segmentManager;

  @BeforeEach
  void beforeEach() {
    storage.logger = mock(Logger.class);
  }

  @Test
  @SuppressWarnings("unchecked")
  void write() {
    String key = "key", value = "value";
    Future<?> mockFuture = mock(Future.class);
    doReturn(mockFuture).when(executorService).submit(any(Callable.class));
    Future<?> future = storage.write(key, value);
    assertEquals(mockFuture, future);
  }

  @Test
  void write_IllegalArgumentException_key() {
    assertThrows(IllegalArgumentException.class, () -> storage.write(null, "value"));
    assertThrows(IllegalArgumentException.class, () -> storage.write("", "value"));
    assertThrows(IllegalArgumentException.class,
        () -> storage.write(new String(new byte[257]), "value"));
  }

  @Test
  void write_IllegalArgumentException_value() {
    assertThrows(IllegalArgumentException.class, () -> storage.write("key", null));
    assertThrows(IllegalArgumentException.class, () -> storage.write("key", ""));
    assertThrows(IllegalArgumentException.class,
        () -> storage.write("key", new String(new byte[257])));
  }

  @Test
  @SuppressWarnings("unchecked")
  void read() {
    String key = "key", value = "value";
    Future<Optional<String>> mockFuture = mock(Future.class);
    doReturn(mockFuture).when(executorService).submit(any(Callable.class));
    Future<Optional<String>> returnedFuture = storage.read(key);
    assertEquals(mockFuture, returnedFuture);
  }

  @Test
  void read_IllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> storage.read(null));
    assertThrows(IllegalArgumentException.class, () -> storage.read(""));
  }
}

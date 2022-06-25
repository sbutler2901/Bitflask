package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageImplTest {

  @InjectMocks
  StorageImpl storage;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentManager segmentManager;

  @Test
  @SuppressWarnings("unchecked")
  void write() throws IOException {
    // Arrange
    String key = "key", value = "value";
    Future<?> mockFuture = mock(Future.class);
    doAnswer((InvocationOnMock invocation) -> {
      Callable<?> writeTask = (Callable<?>) invocation.getArguments()[0];
      writeTask.call();
      return mockFuture;
    }).when(executorService).submit(any(Callable.class));
    // Act
    Future<?> future = storage.write(key, value);
    // Assert
    assertEquals(mockFuture, future);
    verify(segmentManager, times(1)).write(key, value);
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
  void read() throws IOException {
    // Arrange
    String key = "key";
    Future<Optional<String>> mockFuture = mock(Future.class);
    doAnswer((InvocationOnMock invocation) -> {
      Callable<?> writeTask = (Callable<?>) invocation.getArguments()[0];
      writeTask.call();
      return mockFuture;
    }).when(executorService).submit(any(Callable.class));
    // Act
    Future<Optional<String>> returnedFuture = storage.read(key);
    // Assert
    assertEquals(mockFuture, returnedFuture);
    verify(segmentManager, times(1)).read(key);
  }

  @Test
  void read_IllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> storage.read(null));
    assertThrows(IllegalArgumentException.class, () -> storage.read(""));
  }
}

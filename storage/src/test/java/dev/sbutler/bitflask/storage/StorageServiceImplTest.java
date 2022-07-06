package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

  @InjectMocks
  StorageServiceImpl storage;
  @Spy
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  @Mock
  SegmentManager segmentManager;

  @Test
  @SuppressWarnings("unchecked")
  void write() throws Exception {
    // Arrange
    String key = "key", value = "value";
    // Act
    storage.write(key, value);
    // Assert
    verify(executorService, times(1)).submit(any(Callable.class));
    verify(segmentManager, times(1)).write(key, value);
  }

  @Test
  void write_key_invalidArg() {
    assertThrows(NullPointerException.class, () -> storage.write(null, "value"));
    assertThrows(IllegalArgumentException.class, () -> storage.write("", "value"));
    assertThrows(IllegalArgumentException.class,
        () -> storage.write(new String(new byte[257]), "value"));
  }

  @Test
  void write_value_invalidArg() {
    assertThrows(NullPointerException.class, () -> storage.write("key", null));
    assertThrows(IllegalArgumentException.class, () -> storage.write("key", ""));
    assertThrows(IllegalArgumentException.class,
        () -> storage.write("key", new String(new byte[257])));
  }

  @Test
  @SuppressWarnings("unchecked")
  void read() throws Exception {
    // Arrange
    String key = "key";
    // Act
    storage.read(key);
    // Assert
    verify(executorService, times(1)).submit(any(Callable.class));
    verify(segmentManager, times(1)).read(key);
  }

  @Test
  void read_key_invalidArg() {
    assertThrows(NullPointerException.class, () -> storage.read(null));
    assertThrows(IllegalArgumentException.class, () -> storage.read(""));
    assertThrows(IllegalArgumentException.class, () -> storage.read(new String(new byte[257])));
  }
}

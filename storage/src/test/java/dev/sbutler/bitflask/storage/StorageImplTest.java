package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageImplTest {

  @InjectMocks
  StorageImpl storage;
  @Mock
  SegmentManager segmentManager;

  @Test
  void write() throws IOException {
    String key = "key", value = "value";
    storage.write(key, value);
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
  void read() throws IOException {
    String key = "key", value = "value";
    Optional<String> optionalValue = Optional.of(value);
    doReturn(optionalValue).when(segmentManager).read(key);
    Optional<String> readValueOptional = storage.read(key);
    assertEquals(optionalValue, readValueOptional);
  }

  @Test
  void read_IllegalArgumentException() {
    assertThrows(IllegalArgumentException.class, () -> storage.read(null));
    assertThrows(IllegalArgumentException.class, () -> storage.read(""));
  }
}

package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.time.Instant;
import java.util.SortedMap;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** Unit tests for {@link MemtableFactory}. */
public class MemtableFactoryTest {

  private static final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private static final SortedMap<String, Entry> KEY_ENTRY_MAP =
      ImmutableSortedMap.of(ENTRY_0.key(), ENTRY_0);
  private static final StorageConfig STORAGE_CONFIG =
      StorageConfig.newBuilder().setStoreDirectoryPath("/tmp/.bitflask").buildPartial();

  private final WriteAheadLog writeAheadLog = mock(WriteAheadLog.class);
  private final MemtableFactory factory = new MemtableFactory(STORAGE_CONFIG);

  @Test
  public void create() throws Exception {
    try (MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      walMockedStatic.when(() -> WriteAheadLog.create(any())).thenReturn(writeAheadLog);

      Memtable memtable = factory.create();

      assertThat(memtable.getNumBytesSize()).isEqualTo(0);
    }
  }

  @Test
  public void createWithLoading() throws Exception {
    try (MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      walMockedStatic.when(() -> WriteAheadLog.create(any())).thenReturn(writeAheadLog);

      Memtable memtable = factory.createWithLoading(KEY_ENTRY_MAP);

      assertThat(memtable.flush().get(ENTRY_0.key())).isEqualTo(ENTRY_0);
    }
  }
}

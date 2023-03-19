package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.nio.file.Path;
import java.time.Instant;
import java.util.SortedMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class MemtableFactoryTest {

  private static final Path DIR_PATH = Path.of("/tmp/.bitflask");

  private static final long EPOCH_SECONDS_0 = Instant.now().getEpochSecond();

  private static final Entry ENTRY_0 = new Entry(EPOCH_SECONDS_0, "key0", "value0");
  private static final SortedMap<String, Entry> KEY_ENTRY_MAP =
      ImmutableSortedMap.of(ENTRY_0.key(), ENTRY_0);

  private final WriteAheadLog writeAheadLog = mock(WriteAheadLog.class);

  private final StorageConfigurations config = mock(StorageConfigurations.class);
  private final MemtableFactory factory = new MemtableFactory(config);

  @BeforeEach
  public void beforeEach() {
    when(config.getStoreDirectoryPath()).thenReturn(DIR_PATH);
  }

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

package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.StorageLoadingMode;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings({"ResultOfMethodCallIgnored", "resource"})
public class MemtableLoaderTest {

  private static final Path DIR_PATH = Path.of("/tmp/.bitflask");

  private static final long EPOCH_SECONDS_0 = Instant.now().getEpochSecond();
  private static final long EPOCH_SECONDS_1 = Instant.now().getEpochSecond();

  private static final Entry ENTRY_0 = new Entry(EPOCH_SECONDS_0, "key0", "value0");
  private static final Entry ENTRY_1 = new Entry(EPOCH_SECONDS_1, "key1", "value1");

  private final StorageConfigurations config = mock(StorageConfigurations.class);
  private final EntryReader entryReader = mock(EntryReader.class);
  private final WriteAheadLog writeAheadLog = mock(WriteAheadLog.class);

  private final MemtableLoader memtableLoader = new MemtableLoader(config);

  @BeforeEach
  public void beforeEach() {
    when(config.getStoreDirectoryPath()).thenReturn(DIR_PATH);
  }

  @Test
  public void load_withTruncation() {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.TRUNCATE);

    try (MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      walMockedStatic.when(() -> WriteAheadLog.create(any())).thenReturn(writeAheadLog);

      Memtable memtable = memtableLoader.load();

      assertThat(memtable.flush()).isEmpty();
    }
  }

  @Test
  public void load_withTruncation_writeAheadLogThrowsIoException_throwsStorageLoadException() {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.TRUNCATE);

    try (MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      IOException ioException = new IOException("test");
      walMockedStatic.when(() -> WriteAheadLog.create(any())).thenThrow(ioException);

      StorageLoadException e =
          assertThrows(StorageLoadException.class, memtableLoader::load);

      assertThat(e).hasMessageThat().isEqualTo("Failed to create Memtable with truncation");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }

  @Test
  public void load_withLoading_withLoadableEntries_noDuplicates() throws Exception {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.LOAD);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class);
        MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);
      walMockedStatic.when(() -> WriteAheadLog.createFromPreExisting(any()))
          .thenReturn(writeAheadLog);

      when(entryReader.readAllEntriesFromOffset(anyLong()))
          .thenReturn(ImmutableList.of(ENTRY_0, ENTRY_1));

      Memtable memtable = memtableLoader.load();

      assertThat(memtable.read(ENTRY_0.key())).hasValue(ENTRY_0);
      assertThat(memtable.read(ENTRY_1.key())).hasValue(ENTRY_1);
    }
  }

  @Test
  public void load_withLoading_withLoadableEntries_withDuplicates_inOrderByCreation()
      throws Exception {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.LOAD);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class);
        MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);
      walMockedStatic.when(() -> WriteAheadLog.createFromPreExisting(any()))
          .thenReturn(writeAheadLog);

      Entry duplicate = new Entry(EPOCH_SECONDS_1, ENTRY_0.key(), ENTRY_0.value());
      when(entryReader.readAllEntriesFromOffset(anyLong()))
          .thenReturn(ImmutableList.of(ENTRY_0, duplicate));

      Memtable memtable = memtableLoader.load();

      assertThat(memtable.read(ENTRY_0.key())).hasValue(duplicate);
    }
  }

  @Test
  public void load_withLoading_withLoadableEntries_withDuplicates_outOfOrderByCreation()
      throws Exception {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.LOAD);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class);
        MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);
      walMockedStatic.when(() -> WriteAheadLog.createFromPreExisting(any()))
          .thenReturn(writeAheadLog);

      Entry duplicate = new Entry(EPOCH_SECONDS_1, ENTRY_0.key(), ENTRY_0.value());
      when(entryReader.readAllEntriesFromOffset(anyLong()))
          .thenReturn(ImmutableList.of(duplicate, ENTRY_0));

      Memtable memtable = memtableLoader.load();

      assertThat(memtable.read(ENTRY_0.key())).hasValue(duplicate);
    }
  }

  @Test
  public void load_withLoading_withoutLoadableEntries() throws Exception {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.LOAD);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class);
        MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);
      walMockedStatic.when(() -> WriteAheadLog.createFromPreExisting(any()))
          .thenReturn(writeAheadLog);

      when(entryReader.readAllEntriesFromOffset(anyLong())).thenReturn(ImmutableList.of());

      Memtable memtable = memtableLoader.load();

      assertThat(memtable.flush()).isEmpty();
    }
  }

  @Test
  public void load_withLoading_entryReaderThrowsIoException_throwsStorageLoadException()
      throws Exception {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.LOAD);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      IOException ioException = new IOException("test");
      when(entryReader.readAllEntriesFromOffset(anyLong())).thenThrow(ioException);

      StorageLoadException e =
          assertThrows(StorageLoadException.class, memtableLoader::load);

      assertThat(e).hasMessageThat()
          .isEqualTo("Failed to load entries from WriteAheadLog");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }

  @Test
  public void load_withLoading_writeAheadLogThrowsIoException_throwsStorageLoadException()
      throws Exception {
    when(config.getStorageLoadingMode()).thenReturn(StorageLoadingMode.LOAD);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class);
        MockedStatic<WriteAheadLog> walMockedStatic = mockStatic(WriteAheadLog.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);
      when(entryReader.readAllEntriesFromOffset(anyLong())).thenReturn(ImmutableList.of());

      IOException ioException = new IOException("test");
      walMockedStatic.when(() -> WriteAheadLog.createFromPreExisting(any())).thenThrow(ioException);

      StorageLoadException e =
          assertThrows(StorageLoadException.class, memtableLoader::load);

      assertThat(e).hasMessageThat().isEqualTo("Failed to create Memtable with loading");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }

}

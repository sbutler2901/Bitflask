package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.entry.EntryReader;
import java.io.IOException;
import java.time.Instant;
import java.util.SortedMap;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

/** Unit tests for {@link MemtableLoader}. */
@SuppressWarnings({"unchecked"})
public class MemtableLoaderTest {

  private static final StorageConfig STORAGE_CONFIG =
      StorageConfig.newBuilder().setStoreDirectoryPath("/tmp/.bitflask").buildPartial();

  private static final long EPOCH_SECONDS_0 = Instant.now().getEpochSecond();
  private static final long EPOCH_SECONDS_1 = Instant.now().getEpochSecond();

  private static final Entry ENTRY_0 = new Entry(EPOCH_SECONDS_0, "key0", "value0");
  private static final Entry ENTRY_1 = new Entry(EPOCH_SECONDS_1, "key1", "value1");

  private final MemtableFactory memtableFactory = mock(MemtableFactory.class);
  private final EntryReader entryReader = mock(EntryReader.class);
  private final Memtable memtable = mock(Memtable.class);

  @Test
  public void load_withTruncation() throws Exception {
    MemtableLoader memtableLoader =
        createMemtableLoaderWithMode(StorageConfig.LoadingMode.TRUNCATE);
    when(memtableFactory.create()).thenReturn(memtable);

    Memtable createdMemtable = memtableLoader.load();

    assertThat(createdMemtable).isEqualTo(memtable);
  }

  @Test
  public void load_withTruncation_memtableFactoryThrowsIoException_throwsStorageLoadException()
      throws Exception {
    MemtableLoader memtableLoader =
        createMemtableLoaderWithMode(StorageConfig.LoadingMode.TRUNCATE);
    IOException ioException = new IOException("test");
    when(memtableFactory.create()).thenThrow(ioException);

    StorageLoadException e = assertThrows(StorageLoadException.class, memtableLoader::load);

    assertThat(e).hasMessageThat().isEqualTo("Failed to create Memtable with truncation");
    assertThat(e).hasCauseThat().isEqualTo(ioException);
  }

  @Test
  public void load_withLoading_withLoadableEntries_noDuplicates() throws Exception {
    MemtableLoader memtableLoader = createMemtableLoaderWithMode(StorageConfig.LoadingMode.LOAD);
    when(memtableFactory.createWithLoading(any())).thenReturn(memtable);
    when(entryReader.readAllEntriesFromOffset(anyLong()))
        .thenReturn(ImmutableList.of(ENTRY_0, ENTRY_1));

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      Memtable createdMemtable = memtableLoader.load();

      assertThat(createdMemtable).isEqualTo(memtable);
    }

    ArgumentCaptor<SortedMap<String, Entry>> captor = ArgumentCaptor.forClass(SortedMap.class);
    verify(memtableFactory, times(1)).createWithLoading(captor.capture());
    assertThat(captor.getValue().get(ENTRY_0.key())).isEqualTo(ENTRY_0);
    assertThat(captor.getValue().get(ENTRY_1.key())).isEqualTo(ENTRY_1);
  }

  @Test
  public void load_withLoading_withLoadableEntries_withDuplicates_inOrderByCreation()
      throws Exception {
    MemtableLoader memtableLoader = createMemtableLoaderWithMode(StorageConfig.LoadingMode.LOAD);
    when(memtableFactory.createWithLoading(any())).thenReturn(memtable);
    Entry duplicate = new Entry(EPOCH_SECONDS_1, ENTRY_0.key(), ENTRY_0.value());
    when(entryReader.readAllEntriesFromOffset(anyLong()))
        .thenReturn(ImmutableList.of(ENTRY_0, duplicate));

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      Memtable createdMemtable = memtableLoader.load();

      assertThat(createdMemtable).isEqualTo(memtable);
    }

    ArgumentCaptor<SortedMap<String, Entry>> captor = ArgumentCaptor.forClass(SortedMap.class);
    verify(memtableFactory, times(1)).createWithLoading(captor.capture());
    assertThat(captor.getValue().get(ENTRY_0.key())).isEqualTo(duplicate);
  }

  @Test
  public void load_withLoading_withLoadableEntries_withDuplicates_outOfOrderByCreation()
      throws Exception {
    MemtableLoader memtableLoader = createMemtableLoaderWithMode(StorageConfig.LoadingMode.LOAD);
    when(memtableFactory.createWithLoading(any())).thenReturn(memtable);
    Entry duplicate = new Entry(EPOCH_SECONDS_1, ENTRY_0.key(), ENTRY_0.value());
    when(entryReader.readAllEntriesFromOffset(anyLong()))
        .thenReturn(ImmutableList.of(duplicate, ENTRY_0));

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      Memtable createdMemtable = memtableLoader.load();

      assertThat(createdMemtable).isEqualTo(memtable);
    }

    ArgumentCaptor<SortedMap<String, Entry>> captor = ArgumentCaptor.forClass(SortedMap.class);
    verify(memtableFactory, times(1)).createWithLoading(captor.capture());
    assertThat(captor.getValue().get(ENTRY_0.key())).isEqualTo(duplicate);
  }

  @Test
  public void load_withLoading_withoutLoadableEntries() throws Exception {
    MemtableLoader memtableLoader = createMemtableLoaderWithMode(StorageConfig.LoadingMode.LOAD);
    when(memtableFactory.createWithLoading(any())).thenReturn(memtable);
    when(entryReader.readAllEntriesFromOffset(anyLong())).thenReturn(ImmutableList.of());

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      Memtable createdMemtable = memtableLoader.load();

      assertThat(createdMemtable).isEqualTo(memtable);
    }

    ArgumentCaptor<SortedMap<String, Entry>> captor = ArgumentCaptor.forClass(SortedMap.class);
    verify(memtableFactory, times(1)).createWithLoading(captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  public void load_withLoading_entryReaderThrowsIoException_throwsStorageLoadException()
      throws Exception {
    MemtableLoader memtableLoader = createMemtableLoaderWithMode(StorageConfig.LoadingMode.LOAD);
    IOException ioException = new IOException("test");
    when(entryReader.readAllEntriesFromOffset(anyLong())).thenThrow(ioException);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      StorageLoadException e = assertThrows(StorageLoadException.class, memtableLoader::load);

      assertThat(e).hasMessageThat().isEqualTo("Failed to load entries from WriteAheadLog");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }

  @Test
  public void load_withLoading_memtableFactoryThrowsIoException_throwsStorageLoadException()
      throws Exception {
    MemtableLoader memtableLoader = createMemtableLoaderWithMode(StorageConfig.LoadingMode.LOAD);
    when(entryReader.readAllEntriesFromOffset(anyLong())).thenReturn(ImmutableList.of());
    IOException ioException = new IOException("test");
    when(memtableFactory.createWithLoading(any())).thenThrow(ioException);

    try (MockedStatic<EntryReader> entryReaderMockedStatic = mockStatic(EntryReader.class)) {
      entryReaderMockedStatic.when(() -> EntryReader.create(any())).thenReturn(entryReader);

      StorageLoadException e = assertThrows(StorageLoadException.class, memtableLoader::load);

      assertThat(e).hasMessageThat().isEqualTo("Failed to create Memtable with loading");
      assertThat(e).hasCauseThat().isEqualTo(ioException);
    }
  }

  private MemtableLoader createMemtableLoaderWithMode(StorageConfig.LoadingMode loadingMode) {
    return new MemtableLoader(
        STORAGE_CONFIG.toBuilder().setLoadingMode(loadingMode).buildPartial(), memtableFactory);
  }
}

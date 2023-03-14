package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import dev.sbutler.bitflask.storage.lsm.segment.SegmentIndexEntry.PartialEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings("resource")
public class SegmentIndexFactoryTest {

  private static final Path INDEX_PATH = Path.of("/tmp/index_0.idx");
  private static final Path TEST_RESOURCE_PATH = Paths.get("src/test/resources/");

  private static final Entry ENTRY_0 =
      new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private static final Entry ENTRY_1 =
      new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private static final long ENTRY_0_OFFSET = SegmentMetadata.BYTES;
  private static final long ENTRY_1_OFFSET = SegmentMetadata.BYTES + ENTRY_0.getBytes().length;
  private static final ImmutableSortedMap<String, Long> KEY_OFFSET_MAP =
      ImmutableSortedMap.<String, Long>naturalOrder()
          .put(ENTRY_0.key(), ENTRY_0_OFFSET)
          .put(ENTRY_1.key(), ENTRY_1_OFFSET)
          .build();

  private static final UnsignedShort SEGMENT_NUMBER = UnsignedShort.valueOf(0);

  private static final SegmentIndexMetadata METADATA = new SegmentIndexMetadata(SEGMENT_NUMBER);

  private static final SegmentIndexEntry INDEX_ENTRY_0 =
      new SegmentIndexEntry(ENTRY_0.key(), ENTRY_0_OFFSET);
  private static final SegmentIndexEntry INDEX_ENTRY_1 =
      new SegmentIndexEntry(ENTRY_1.key(), ENTRY_1_OFFSET);

  private final StorageConfigurations config = mock(StorageConfigurations.class);

  private SegmentIndexFactory indexFactory;

  @BeforeEach
  public void beforeEach() {
    when(config.getStorageStoreDirectoryPath()).thenReturn(TEST_RESOURCE_PATH);
    indexFactory = new SegmentIndexFactory(config);
  }

  @Test
  public void create() throws Exception {
    SegmentIndex segmentIndex;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      segmentIndex = indexFactory.create(KEY_OFFSET_MAP, SEGMENT_NUMBER);
    }

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        METADATA.getBytes(),
        INDEX_ENTRY_0.getBytes(),
        INDEX_ENTRY_1.getBytes()));

    assertThat(segmentIndex.getSegmentNumber()).isEqualTo(SEGMENT_NUMBER.value());
    assertThat(segmentIndex.getKeyOffset(ENTRY_0.key())).hasValue(ENTRY_0_OFFSET);
    assertThat(segmentIndex.getKeyOffset(ENTRY_1.key())).hasValue(ENTRY_1_OFFSET);
  }

  @Test
  public void loadFromPath_success() throws Exception {
    ByteArrayInputStream is = new ByteArrayInputStream(Bytes.concat(
        METADATA.getBytes(),
        INDEX_ENTRY_0.getBytes(),
        INDEX_ENTRY_1.getBytes()));

    SegmentIndex segmentIndex;
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(is);

      segmentIndex = indexFactory.loadFromPath(INDEX_PATH);
    }

    assertThat(segmentIndex.getSegmentNumber()).isEqualTo(SEGMENT_NUMBER.value());
    assertThat(segmentIndex.getKeyOffset(ENTRY_0.key())).hasValue(ENTRY_0_OFFSET);
    assertThat(segmentIndex.getKeyOffset(ENTRY_1.key())).hasValue(ENTRY_1_OFFSET);
  }

  @Test
  public void loadFromPath_emptyFile_throwsStorageLoadException() {
    ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{});

    StorageLoadException e;
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(is);

      e = assertThrows(StorageLoadException.class, () -> indexFactory.loadFromPath(INDEX_PATH));
    }

    assertThat(e).hasMessageThat().isEqualTo(String.format(
        "SegmentIndex SegmentIndexMetadata bytes read too short. Expected [%d], actual [%d]",
        SegmentIndexMetadata.BYTES, 0));
  }

  @Test
  public void loadFromPath_partialEntryBytesTooShort_throwsStorageLoadException() {
    ByteArrayInputStream is = new ByteArrayInputStream(Bytes.concat(
        METADATA.getBytes(),
        new byte[PartialEntry.BYTES - 1]));

    StorageLoadException e;
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(is);

      e = assertThrows(StorageLoadException.class, () -> indexFactory.loadFromPath(INDEX_PATH));
    }

    assertThat(e).hasMessageThat().isEqualTo(String.format(
        "SegmentIndex PartialEntry bytes read too short. Expected [%d], actual [%d]",
        PartialEntry.BYTES, PartialEntry.BYTES - 1));
  }

  @Test
  public void loadFromPath_keyBytesTooShort_throwsStorageLoadException() {
    int keyLength = 3;
    byte[] keyBytes = "ke".getBytes(StandardCharsets.UTF_8);
    ByteArrayInputStream is = new ByteArrayInputStream(Bytes.concat(
        METADATA.getBytes(),
        new PartialEntry(UnsignedShort.valueOf(keyLength), 0).getBytes(),
        keyBytes));

    StorageLoadException e;
    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any())).thenReturn(is);

      e = assertThrows(StorageLoadException.class, () -> indexFactory.loadFromPath(INDEX_PATH));
    }

    assertThat(e).hasMessageThat().isEqualTo(String.format(
        "SegmentIndex key bytes read too short. Expected [%d], actual [%d]",
        keyLength, keyBytes.length));
  }
}

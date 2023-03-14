package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

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
  public void createSegmentIndex() throws Exception {
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
}

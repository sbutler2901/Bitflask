package dev.sbutler.bitflask.storage.segment;

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
import dev.sbutler.bitflask.storage.entry.Entry;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class SegmentIndexFactoryTest {

  private final Path TEST_RESOURCE_PATH = Paths.get("src/test/resources/");
  private final StorageConfigurations CONFIG = mock(StorageConfigurations.class);

  private final Entry ENTRY_0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private final Entry ENTRY_1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private final UnsignedShort SEGMENT_NUMBER = UnsignedShort.valueOf(0);

  private SegmentIndexFactory indexFactory;

  @BeforeEach
  public void beforeEach() {
    when(CONFIG.getStorageStoreDirectoryPath()).thenReturn(TEST_RESOURCE_PATH);
    indexFactory = new SegmentIndexFactory(CONFIG);
  }

  @Test
  public void createSegmentIndex() throws Exception {
    long entryOffset0 = SegmentMetadata.BYTES;
    long entryOffset1 = entryOffset0 + ENTRY_0.getBytes().length;
    ImmutableSortedMap<String, Long> keyOffsetMap = ImmutableSortedMap.<String, Long>naturalOrder()
        .put(ENTRY_0.key(), entryOffset0).put(ENTRY_1.key(), entryOffset1).build();

    SegmentIndex segmentIndex;

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any())).thenReturn(outputStream);

      segmentIndex = indexFactory.create(keyOffsetMap, SEGMENT_NUMBER);
    }

    assertThat(outputStream.toByteArray()).isEqualTo(Bytes.concat(
        new SegmentIndexMetadata(SEGMENT_NUMBER).getBytes(),
        new SegmentIndexEntry(ENTRY_0.key(), entryOffset0).getBytes(),
        new SegmentIndexEntry(ENTRY_1.key(), entryOffset1).getBytes()));

    assertThat(segmentIndex.getSegmentNumber()).isEqualTo(SEGMENT_NUMBER.value());
    assertThat(segmentIndex.getKeyOffset(ENTRY_0.key())).hasValue(entryOffset0);
    assertThat(segmentIndex.getKeyOffset(ENTRY_1.key())).hasValue(entryOffset1);
  }
}

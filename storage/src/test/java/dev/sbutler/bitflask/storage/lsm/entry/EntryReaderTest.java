package dev.sbutler.bitflask.storage.lsm.entry;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import dev.sbutler.bitflask.storage.lsm.segment.Segment;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

@SuppressWarnings({"resource"})
public class EntryReaderTest {

  private final static Path FILE_PATH = Paths.get(
      "src/test/resources/segment0" + Segment.FILE_EXTENSION);

  private final EntryReader entryReader = EntryReader.create(FILE_PATH);

  @Test
  public void findEntryFromOffset_found() throws Exception {
    String key = "key";
    String value = "value";

    Entry storedEntry = new Entry(Instant.now().getEpochSecond(), key, value);
    InputStream is = new ByteArrayInputStream(storedEntry.getBytes());

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.findEntryFromOffset(key, 0L);
      assertThat(entry).hasValue(storedEntry);
    }
  }

  @Test
  public void findEntryFromOffset_found_noOffset() throws Exception {
    String key0 = "key0", key1 = "key1";
    String value0 = "value0", value1 = "value1";

    Entry storedEntry0 = new Entry(Instant.now().getEpochSecond(), key0, value0);
    Entry storedEntry1 = new Entry(Instant.now().getEpochSecond(), key1, value1);

    InputStream is = new ByteArrayInputStream(Bytes.concat(
        storedEntry0.getBytes(),
        storedEntry1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.
          findEntryFromOffset(key1, storedEntry0.getBytes().length);
      assertThat(entry).hasValue(storedEntry1);
    }
  }

  @Test
  public void findEntryFromOffset_found_skipToOffset() throws Exception {
    String key0 = "key0", key1 = "key1";
    String value0 = "value0", value1 = "value1";

    Entry storedEntry0 = new Entry(Instant.now().getEpochSecond(), key0, value0);
    Entry storedEntry1 = new Entry(Instant.now().getEpochSecond(), key1, value1);

    InputStream is = new ByteArrayInputStream(Bytes.concat(
        storedEntry0.getBytes(),
        storedEntry1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.
          findEntryFromOffset(key1, storedEntry0.getBytes().length);
      assertThat(entry).hasValue(storedEntry1);
    }
  }

  @Test
  public void findEntryFromOffset_found_numberReadMismatch_key() {
    String key = "key";
    String value = "value";

    EntryMetadata storedMetadata = new EntryMetadata(
        Instant.now().getEpochSecond(),
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
    InputStream is = new ByteArrayInputStream(storedMetadata.getBytes());

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      IOException e =
          assertThrows(IOException.class,
              () -> entryReader.findEntryFromOffset(key, 0L));

      assertThat(e).hasMessageThat().ignoringCase()
          .contains("Read key length did not match entry.");
    }
  }

  @Test
  public void findEntryFromOffset_found_numberReadMismatch_value() {
    String key = "key";
    String value = "value";

    EntryMetadata storedMetadata = new EntryMetadata(
        Instant.now().getEpochSecond(),
        UnsignedShort.valueOf(key.length()),
        UnsignedShort.valueOf(value.length()));
    InputStream is = new ByteArrayInputStream(Bytes.concat(
        storedMetadata.getBytes(),
        key.getBytes(StandardCharsets.UTF_8)));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      IOException e =
          assertThrows(IOException.class,
              () -> entryReader.findEntryFromOffset(key, 0L));

      assertThat(e).hasMessageThat().ignoringCase()
          .contains("Read value length did not match entry.");
    }
  }

  @Test
  public void findEntryFromOffset_emptyFile() throws Exception {
    String key = "key";
    InputStream is = new ByteArrayInputStream(new byte[0]);

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.findEntryFromOffset(key, 0L);
      assertThat(entry).isEmpty();
    }
  }

  @Test
  public void findEntryFromOffset_notFound() throws Exception {
    String key = "key";
    String value = "value";

    Entry storedEntry = new Entry(Instant.now().getEpochSecond(), key, value);
    InputStream is = new ByteArrayInputStream(storedEntry.getBytes());

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.findEntryFromOffset("absent-key", 0L);
      assertThat(entry).isEmpty();
    }
  }
}

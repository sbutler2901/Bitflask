package dev.sbutler.bitflask.storage.lsm.entry;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import com.google.common.collect.ImmutableList;
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

  private static final Entry ENTRY_0 =
      new Entry(Instant.now().getEpochSecond(), "key0", "value0");
  private static final Entry ENTRY_1 =
      new Entry(Instant.now().getEpochSecond(), "key1", "value1");

  private final EntryReader entryReader = EntryReader.create(FILE_PATH);

  @Test
  public void readAllEntriesFromOffset() throws Exception {
    InputStream is = new ByteArrayInputStream(Bytes.concat(
        ENTRY_0.getBytes(),
        ENTRY_1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      ImmutableList<Entry> entries = entryReader.readAllEntriesFromOffset(0L);
      assertThat(entries).containsExactly(ENTRY_0, ENTRY_1);
    }
  }

  @Test
  public void readAllEntriesFromOffset_skipToOffset() throws Exception {
    InputStream is = new ByteArrayInputStream(Bytes.concat(
        ENTRY_0.getBytes(),
        ENTRY_1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      ImmutableList<Entry> entries =
          entryReader.readAllEntriesFromOffset(ENTRY_0.getBytes().length);
      assertThat(entries).containsExactly(ENTRY_1);
    }
  }

  @Test
  public void readAllEntriesFromOffset_numberReadMismatch_key() {
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
              () -> entryReader.readAllEntriesFromOffset(0L));

      assertThat(e).hasMessageThat().ignoringCase()
          .contains("Read key length did not match entry.");
    }
  }

  @Test
  public void readAllEntriesFromOffset_numberReadMismatch_value() {
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
              () -> entryReader.readAllEntriesFromOffset(0L));

      assertThat(e).hasMessageThat().ignoringCase()
          .contains("Read value length did not match entry.");
    }
  }

  @Test
  public void readAllEntriesFromOffset_emptyFile() throws Exception {
    InputStream is = new ByteArrayInputStream(new byte[0]);

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      ImmutableList<Entry> entries = entryReader.readAllEntriesFromOffset(0L);
      assertThat(entries).isEmpty();
    }
  }

  @Test
  public void findEntryFromOffset_found() throws Exception {
    InputStream is = new ByteArrayInputStream(ENTRY_0.getBytes());

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.findEntryFromOffset(ENTRY_0.key(), 0L);
      assertThat(entry).hasValue(ENTRY_0);
    }
  }

  @Test
  public void findEntryFromOffset_found_noOffset() throws Exception {
    InputStream is = new ByteArrayInputStream(Bytes.concat(
        ENTRY_0.getBytes(),
        ENTRY_1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader.findEntryFromOffset(ENTRY_1.key(), 0L);
      assertThat(entry).hasValue(ENTRY_1);
    }
  }

  @Test
  public void findEntryFromOffset_found_skipToOffset() throws Exception {
    InputStream is = new ByteArrayInputStream(Bytes.concat(
        ENTRY_0.getBytes(),
        ENTRY_1.getBytes()));

    try (MockedStatic<Files> filesMockedStatic = mockStatic(Files.class)) {
      filesMockedStatic.when(() -> Files.newInputStream(any(), any())).thenReturn(is);
      Optional<Entry> entry = entryReader
          .findEntryFromOffset(ENTRY_1.key(), ENTRY_0.getBytes().length);
      assertThat(entry).hasValue(ENTRY_1);
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

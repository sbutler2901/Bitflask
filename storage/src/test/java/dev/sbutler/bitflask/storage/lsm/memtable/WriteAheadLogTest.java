package dev.sbutler.bitflask.storage.lsm.memtable;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.primitives.Bytes;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.lsm.entry.Entry;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class WriteAheadLogTest {

  private final Path TEST_RESOURCE_PATH = Paths.get("src/test/resources/");

  private final StorageConfigurations config = mock(StorageConfigurations.class);


  @BeforeEach
  public void beforeEach() throws Exception {
    when(config.getStorageStoreDirectoryPath()).thenReturn(TEST_RESOURCE_PATH);
  }

  @Test
  public void append_newFile() throws Exception {
    // Arrange
    Entry entry = new Entry(Instant.now().getEpochSecond(), "key", "value");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    WriteAheadLog writeAheadLog;
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any(StandardOpenOption[].class)))
          .thenReturn(outputStream);
      writeAheadLog = WriteAheadLog.create(TEST_RESOURCE_PATH);
    }

    // Act
    writeAheadLog.append(entry);
    writeAheadLog.close();
    // Assert
    assertThat(outputStream.toByteArray()).isEqualTo(entry.getBytes());
  }

  @Test
  public void append_preExistingFile() throws Exception {
    // Arrange
    Entry entry0 = new Entry(Instant.now().getEpochSecond(), "key0", "value0");
    Entry entry1 = new Entry(Instant.now().getEpochSecond(), "key1", "value1");
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputStream.write(entry0.getBytes());

    WriteAheadLog writeAheadLog;
    try (MockedStatic<Files> fileMockedStatic = mockStatic(Files.class)) {
      fileMockedStatic.when(() -> Files.newOutputStream(any(), any(StandardOpenOption[].class)))
          .thenReturn(outputStream);
      writeAheadLog = WriteAheadLog.create(TEST_RESOURCE_PATH);
    }

    // Act
    writeAheadLog.append(entry1);
    writeAheadLog.close();
    // Assert
    assertThat(outputStream.toByteArray())
        .isEqualTo(Bytes.concat(entry0.getBytes(), entry1.getBytes()));
  }
}

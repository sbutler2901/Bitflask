package dev.sbutler.bitflask.storage.segmentV1;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.segmentV1.SegmentFile.Factory;
import dev.sbutler.bitflask.storage.segmentV1.SegmentFile.Header;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentFileTest {

  private SegmentFile segmentFile;
  private FileChannel fileChannel;
  private Header header;
  private final Path path = Path.of("test-path");

  @BeforeEach
  void beforeEach() {
    fileChannel = mock(FileChannel.class);
    SegmentFile.Factory segmentFileFactory = new Factory();
    header = new Header((char) 0);
    segmentFile = segmentFileFactory.create(fileChannel, path, header);
  }

  @Test
  void write() throws Exception {
    // Act
    segmentFile.write(new byte[]{'a'}, 0L);
    // Assert
    verify(fileChannel, times(1)).write(any(ByteBuffer.class), anyLong());
  }

  @Test
  void read() throws Exception {
    // Act
    segmentFile.read(0, 0L);
    // Assert
    verify(fileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  void readAsString() throws Exception {
    // Act
    String result = segmentFile.readAsString(0, 0L);
    // Assert
    assertThat(result).hasLength(0);
    verify(fileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  void readByte() throws Exception {
    // Act
    segmentFile.readByte(0L);
    // Assert
    verify(fileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  void size() throws Exception {
    // Arrange
    when(fileChannel.size()).thenReturn(0L);
    // Act
    long size = segmentFile.size();
    // Assert
    assertThat(size).isEqualTo(0L);
  }

  @Test
  void truncate() throws Exception {
    // Act
    segmentFile.truncate(10);
    // Assert
    verify(fileChannel, times(1)).truncate(10);
  }

  @Test
  void getSegmentFilePath() {
    // Act
    Path segmentFilePath = segmentFile.getSegmentFilePath();
    // Assert
    assertThat(segmentFilePath.equals(path)).isTrue();
  }

  @Test
  void getSegmentFileKey() {
    // Act
    int key = segmentFile.getSegmentFileKey();
    // Assert
    assertThat(key).isEqualTo(header.key());
  }

  @Test
  void close() throws Exception {
    // Act
    segmentFile.close();
    // Assert
    verify(fileChannel, times(1)).close();
  }

  @Test
  void close_IOException() throws Exception {
    // Arrange
    doThrow(IOException.class).when(fileChannel).close();
    // Act
    segmentFile.close();
    // Assert
    verify(fileChannel, times(1)).close();
  }

  @Test
  void isOpen() {
    // Arrange
    when(fileChannel.isOpen()).thenReturn(true).thenReturn(false);
    // Act / Assert
    /// open
    assertThat(segmentFile.isOpen()).isTrue();
    /// closed
    assertThat(segmentFile.isOpen()).isFalse();
  }

  @Test
  void header_readFromFileChannel() throws Exception {
    // Note: can't mock ByteBuffer: must rely on fact initialized with values set to 0
    // Arrange
    FileChannel fileChannel = mock(FileChannel.class);
    // Act
    Header header = Header.readHeaderFromFileChannel(fileChannel);
    // Assert
    assertThat(header.key()).isEqualTo(0);
  }
}

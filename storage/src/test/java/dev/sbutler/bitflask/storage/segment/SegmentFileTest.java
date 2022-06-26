package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SegmentFileTest {

  SegmentFile segmentFile;
  FileChannel fileChannel;
  Path path = Path.of("test-path");
  int segmentFileKey = 0;

  @BeforeEach
  void beforeEach() {
    fileChannel = mock(FileChannel.class);
    segmentFile = new SegmentFile(fileChannel, path, segmentFileKey);
  }

  @Test
  void write() throws IOException {
    segmentFile.write(new byte[]{'a'}, 0L);
    verify(fileChannel, times(1)).write(any(ByteBuffer.class), anyLong());
  }

  @Test
  void write_exception() throws IOException {
    doThrow(IOException.class).when(fileChannel).write(any(ByteBuffer.class), anyLong());
    assertThrows(IOException.class, () -> segmentFile.write(new byte[]{'a'}, 0L));
  }

  @Test
  void read() throws IOException {
    segmentFile.read(0, 0L);
    verify(fileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  void read_exception() throws IOException {
    doThrow(IOException.class).when(fileChannel).read(any(ByteBuffer.class), anyLong());

    assertThrows(IOException.class, () -> segmentFile.read(0, 0L));
  }

  @Test
  void readAsString() throws IOException {
    String result = segmentFile.readAsString(0, 0L);
    assertEquals(0, result.length());
    verify(fileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  void readByte() throws IOException {
    byte result = segmentFile.readByte(0L);
    verify(fileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  void size() throws IOException {
    doReturn(0L).when(fileChannel).size();
    assertEquals(0L, segmentFile.size());
  }

  @Test
  void getSegmentFilePath() {
    assertEquals(path, segmentFile.getSegmentFilePath());
    assertEquals(segmentFileKey, segmentFile.getSegmentFileKey());
  }

  @Test
  void getSegmentFileKey() {
    assertEquals(segmentFileKey, segmentFile.getSegmentFileKey());
  }

  @Test
  void close() throws IOException {
    segmentFile.close();
    verify(fileChannel, times(1)).close();
  }

  @Test
  void close_IOException() throws IOException {
    doThrow(IOException.class).when(fileChannel).close();
    segmentFile.close();
    verify(fileChannel, times(1)).close();
  }

  @Test
  void isOpen() {
    // open
    doReturn(true).when(fileChannel).isOpen();
    assertTrue(segmentFile.isOpen());

    // closed
    doReturn(false).when(fileChannel).isOpen();
    assertFalse(segmentFile.isOpen());
  }
}

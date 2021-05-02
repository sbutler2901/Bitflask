package com.ibm.sbutler.bitflask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.RandomAccessFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageTest {
  @InjectMocks
  Storage storage;

  @Mock
  RandomAccessFile randomAccessFile;

  @Test
  void write_badBuffer() {
    assertThrows(IllegalArgumentException.class, () -> storage.write(null, 0));
  }

  @Test
  void write_emptyBuffer() {
    byte[] bytes = new byte[0];
    assertThrows(IllegalArgumentException.class, () -> storage.write(bytes, 0));
  }

  @Test
  void write_invalidOffset() {
    byte[] bytes = new byte[10];
    assertThrows(IllegalArgumentException.class, () -> storage.write(bytes, -1));
  }

  @Test
  void write_fileException() throws IOException {
    byte[] bytes = new byte[10];
    doThrow(new IOException()).when(randomAccessFile).seek(anyLong());
    assertThrows(IOException.class, () -> storage.write(bytes, 10));
  }

  @Test
  void read_badBuffer() {
    assertThrows(IllegalArgumentException.class, () -> storage.read(null, 0));
  }

  @Test
  void read_emptyBuffer() {
    byte[] bytes = new byte[0];
    assertThrows(IllegalArgumentException.class, () -> storage.read(bytes, 0));
  }

  @Test
  void read_invalidOffset() {
    byte[] bytes = new byte[10];
    assertThrows(IllegalArgumentException.class, () -> storage.read(bytes, -1));
  }

  @Test
  void read_endOfFile() throws Exception {
    byte[] bytes = new byte[10];
    when(randomAccessFile.read(any())).thenReturn(-1);
    storage.read(bytes, 0);
  }

  @Test
  void read_noLength() throws Exception {
    byte[] bytes = new byte[10];
    when(randomAccessFile.read(any())).thenReturn(0);
    storage.read(bytes, 0);
  }

  @Test
  void read_differentLength() throws Exception {
    byte[] bytes = new byte[10];
    when(randomAccessFile.read(any())).thenReturn(5);
    storage.read(bytes, 0);
  }
}
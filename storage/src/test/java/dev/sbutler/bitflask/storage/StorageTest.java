//package com.ibm.sbutler.bitflask.Storage;
//
//import dev.sbutler.bitflask.server.storage.Storage;
//import dev.sbutler.bitflask.server.storage.StorageEntry;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.io.IOException;
//import java.io.RandomAccessFile;
//import java.nio.charset.StandardCharsets;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class StorageTest {
//  @InjectMocks
//  Storage storage;
//
//  @Mock
//  RandomAccessFile randomAccessFile;
//
//  @Test
//  void write_success() throws IOException {
//    String toWrite = "test";
//    StorageEntry expectedStorageEntry = new StorageEntry(0, 0, toWrite.length());
//    when(randomAccessFile.length()).thenReturn(0L);
//    StorageEntry returnedStorageEntry = storage.write(toWrite.getBytes(StandardCharsets.UTF_8));
//    assertEquals(expectedStorageEntry.getSegmentIndex(), returnedStorageEntry.getSegmentIndex());
//    assertEquals(expectedStorageEntry.getSegmentOffset(), returnedStorageEntry.getSegmentOffset());
//    assertEquals(expectedStorageEntry.getEntryLength(), returnedStorageEntry.getEntryLength());
//  }
//
//  @Test
//  void write_threshold() throws IOException {
//    String toWrite = "test";
//    StorageEntry expectedStorageEntry = new StorageEntry(0, Storage.NEW_SEGMENT_THRESHOLD, toWrite.length());
//    when(randomAccessFile.length()).thenReturn(Storage.NEW_SEGMENT_THRESHOLD);
//    StorageEntry returnedStorageEntry = storage.write(toWrite.getBytes(StandardCharsets.UTF_8));
//    assertEquals(expectedStorageEntry.getSegmentIndex(), returnedStorageEntry.getSegmentIndex());
//    assertEquals(expectedStorageEntry.getSegmentOffset(), returnedStorageEntry.getSegmentOffset());
//    assertEquals(expectedStorageEntry.getEntryLength(), returnedStorageEntry.getEntryLength());
//    // segment threshold hit
//    assertEquals(1, storage.getActiveSegmentFileIndex());
//  }
//
//  @Test
//  void write_badBuffer() {
//    // null
//    assertThrows(IllegalArgumentException.class, () -> storage.write(null));
//    // empty
//    byte[] bytes = new byte[0];
//    assertThrows(IllegalArgumentException.class, () -> storage.write(bytes));
//  }
//
//  @Test
//  void write_fileException() throws IOException {
//    byte[] bytes = new byte[10];
//    doThrow(new IOException()).when(randomAccessFile).seek(anyLong());
//    assertThrows(IOException.class, () -> storage.write(bytes));
//  }
//
//  @Test
//  void read_success() throws IOException {
//    int entryLength = 10;
//    StorageEntry storageEntry = new StorageEntry(0, 0, entryLength);
//    when(randomAccessFile.length()).thenReturn(100L);
//    when(randomAccessFile.read(any())).thenReturn(entryLength);
//
//    byte[] readBytes = storage.read(storageEntry);
//    assertEquals(entryLength, readBytes.length);
//  }
//
//  @Test
//  void read_badEntry() {
//    // null
//    assertThrows(IllegalArgumentException.class, () -> storage.read(null));
//    // segmentIndex too large
//    assertThrows(IllegalArgumentException.class, () -> storage.read(new StorageEntry(10, 0, 10)));
//    // segmentOffset too large
//    assertThrows(IllegalArgumentException.class, () -> storage.read(new StorageEntry(0, 10, 10)));
//    // segmentOffset & length too large
//    assertThrows(IllegalArgumentException.class, () -> storage.read(new StorageEntry(0, 0, 10)));
//  }
//
//  @Test
//  void read_endOfFile() throws Exception {
//    when(randomAccessFile.length()).thenReturn(100L);
//    when(randomAccessFile.read(any())).thenReturn(-1);
//    storage.read(new StorageEntry(0, 0, 10));
//  }
//
//  @Test
//  void read_noLength() throws Exception {
//    when(randomAccessFile.length()).thenReturn(100L);
//    when(randomAccessFile.read(any())).thenReturn(0);
//    storage.read(new StorageEntry(0, 0, 10));
//  }
//
//  @Test
//  void read_differentLength() throws Exception {
//    when(randomAccessFile.length()).thenReturn(100L);
//    when(randomAccessFile.read(any())).thenReturn(5);
//    storage.read(new StorageEntry(0, 0, 10));
//  }
//}
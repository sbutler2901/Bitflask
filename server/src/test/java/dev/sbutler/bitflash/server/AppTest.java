//package com.ibm.sbutler.bitflask;
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
//import java.nio.charset.StandardCharsets;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class AppTest {
//  @InjectMocks
//  App app;
//
//  @Mock
//  Storage storage;
//
//  @Test
//  void set_success() throws IOException {
//    String key = "testKey";
//    String expectedValue = "value";
//    byte[] expectedBytes = expectedValue.getBytes(StandardCharsets.UTF_8);
//
//    app.set(key, expectedValue);
//    verify(storage).write(eq(expectedBytes));
//  }
//
//  @Test
//  void set_ioExceptions() throws IOException {
//    doThrow(new IOException()).when(storage).write(any(byte[].class));
//    assertThrows(IOException.class, () -> app.set("key", "value"));
//  }
//
//  @Test
//  void get_success() throws IOException {
//    String key = "testKey";
//    String expectedValue = "testValue";
//    StorageEntry storageEntry = new StorageEntry(0, 0, expectedValue.length());
//
//    when(storage.write(any(byte[].class))).thenReturn(storageEntry);
//    app.set(key, expectedValue);
//    when(storage.read(any(StorageEntry.class))).thenReturn(expectedValue.getBytes(StandardCharsets.UTF_8));
//    String result = app.get(key);
//    assertEquals(expectedValue, result);
//  }
//
//  @Test
//  void get_ioExceptions() throws IOException {
//    String key = "testKey";
//    StorageEntry storageEntry = new StorageEntry(0, 0, 10);
//    when(storage.write(any(byte[].class))).thenReturn(storageEntry);
//    app.set(key, "value");
//    doThrow(new IOException()).when(storage).read(any(StorageEntry.class));
//    assertThrows(IOException.class, () -> app.get(key));
//  }
//
//  @Test
//  void get_nullKey() throws IOException {
//    app.set("testKey", "");
//    assertNull(app.get(null));
//  }
//}
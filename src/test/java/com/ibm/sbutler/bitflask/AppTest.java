package com.ibm.sbutler.bitflask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppTest {
  @InjectMocks
  App app;

  @Mock
  Storage storage;

  @Test
  void set_success() throws IOException {
    String key = "testKey";
    String expectedValue = "value";
    long expectedOffset = 0;
    byte[] expectedBytes = expectedValue.getBytes(StandardCharsets.UTF_8);

    // new key
    app.set(key, expectedValue);
    verify(storage).write(eq(expectedBytes), eq(expectedOffset));

    // preexisting key
    app.set(key, expectedValue);
    verify(storage).write(eq(expectedBytes), eq(expectedOffset + expectedValue.length()));
  }

  @Test
  void set_ioExceptions() throws IOException {
    doThrow(new IOException()).when(storage).write(any(byte[].class), anyLong());
    assertThrows(IOException.class, () -> app.set("key", "value"));
  }

  @Test
  void get_success() throws IOException {
    String key = "testKey";
    String expectedValue = "value";
    long expectedOffset = 0;

    app.set(key, expectedValue);
    verify(storage).read(any(byte[].class), eq(expectedOffset));
  }

  @Test
  void get_ioExceptions() throws IOException {
    String key = "testKey";
    app.set(key, "value");
    doThrow(new IOException()).when(storage).read(any(byte[].class), anyLong());
    assertThrows(IOException.class, () -> app.get(key));
  }

  @Test
  void get_nullKey() throws IOException {
    app.set("testKey", "");
    assertNull(app.get(null));
  }

  @Test
  void get_emptyValue() throws IOException {
    String key = "testKey";
    app.set(key, "");
    assertNull(app.get(key));
  }
}
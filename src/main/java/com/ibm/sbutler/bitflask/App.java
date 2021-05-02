package com.ibm.sbutler.bitflask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class App {
  private static final String SAVED_LOG = "Saved (%s) with value (%s) to offset (%d)";
  private static final String READ_LOG = "Read (%s) with value (%s) at offset (%d)";
  private static final String READ_ERR_KEY_NOT_FOUND = "Error reading (%s), offset entry not found";

  private final Storage storage;

  private final Map<String, Entry> offsetMap = new HashMap<>();
  private int currentOffset = 0;

  @AllArgsConstructor
  private static class Entry {
    @Getter
    @Setter
    private int offset, length;
  }

  App() throws FileNotFoundException {
    this(new Storage());
  }

  App(Storage storage) {
    this.storage = storage;
  }

  public void set(String key, String value) throws IOException {
    storage.write(value.getBytes(StandardCharsets.UTF_8), currentOffset);

    if (offsetMap.containsKey(key)) {
      Entry entry = offsetMap.get(key);
      entry.setOffset(currentOffset);
      entry.setLength(value.length());
    } else {
      Entry entry = new Entry(currentOffset, value.length());
      offsetMap.put(key, entry);
    }

    currentOffset += value.length();
    System.out.printf((SAVED_LOG) + "%n", key, value, currentOffset);
  }

  public String get(String key) throws IOException {
    Entry entry = offsetMap.get(key);

    if (entry == null) {
      System.out.printf((READ_ERR_KEY_NOT_FOUND) + "%n", key);
      return null;
    }

    byte[] bytes = new byte[entry.getLength()];
    storage.read(bytes, entry.getOffset());
    String value = new String(bytes);

    System.out.printf((READ_LOG) + "%n", key, value, entry.offset);

    if (value.length() > 0) {
      return value;
    }
    return null;
  }
}

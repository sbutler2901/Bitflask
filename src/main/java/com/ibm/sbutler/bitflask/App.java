package com.ibm.sbutler.bitflask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class App {
  private final String SAVED_LOG = "Saved (%s) with value (%s) to offset (%d)";
  private final String READ_LOG = "Read (%s) with value (%s) at offset (%d)";
  private final String READ_ERR_KEY_NOT_FOUND = "Error reading (%s), offset entry not found";
  private final String READ_ERR_NO_VALUE = "Error reading (%s), end of file";
  private final String READ_ERR_NO_LENGTH = "Error reading (%s), length 0";

  private final RandomAccessFile randomAccessFile;

  private final Map<String, Entry> offsetMap = new HashMap<>();
  private int currentOffset = 0;

  @AllArgsConstructor
  private static class Entry {
    @Getter
    @Setter
    private int offset, length;
  }

  App(String filePath) throws IOException {
    randomAccessFile = new RandomAccessFile(filePath, "rw");
  }

  public String get(String key) {
    Entry entry = offsetMap.get(key);

    if (entry == null) {
      System.out.printf((READ_ERR_KEY_NOT_FOUND) + "%n", key);
      return null;
    }

    try {
      randomAccessFile.seek(entry.getOffset());

      byte[] bytes = new byte[entry.getLength()];
      int result = randomAccessFile.read(bytes);
      String value = new String(bytes);

      if (result < 0) {
        System.out.printf((READ_ERR_NO_VALUE) + "%n", key);
      } else if (result == 0) {
        System.out.printf((READ_ERR_NO_LENGTH) + "%n", key);
      } else {
        System.out.printf((READ_LOG) + "%n", key, value, entry.offset);
      }

      if (value.length() > 0) {
        return value;
      } else {
        return null;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  public void save(String key, String value) {
    try {
      randomAccessFile.seek(currentOffset);
      randomAccessFile.write(value.getBytes(StandardCharsets.UTF_8));

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
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

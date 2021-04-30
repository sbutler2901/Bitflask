package com.ibm.sbutler.bitflask;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class App {
  private final String SAVED_LOG = "Saved (%s) with value (%s) to offset (%d)";
  private final String READ_LOG = "Read (%s) with value (%s) at offset (%d)";
  private final String READ_ERR_KEY_NOT_FOUND = "Error reading (%s), offset entry not found";
  private final String READ_ERR_NO_VALUE = "Error reading (%s), end of file";
  private final String READ_ERR_NO_LENGTH = "Error reading (%s), length 0";

  private final OutputStream outputStream;
//  private final InputStream inputStream;
  private final Map<String, Entry> offsetMap = new HashMap<>();
  private int currentOffset = 0;

  private final Path path;

  @AllArgsConstructor
  private static class Entry {
    @Getter
    @Setter
    private int offset, length;
  }

  App(String filePath) throws IOException {
    path = Paths.get(filePath);
    outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE);
//    inputStream = Files.newInputStream(path);
  }

  public String get(String key) {
    Entry entry = offsetMap.get(key);

    if (entry == null) {
      System.out.printf((READ_ERR_KEY_NOT_FOUND) + "%n", key);
      return null;
    }

    try {
      InputStream inputStream = Files.newInputStream(path);
      inputStream.skip(entry.getOffset());

      byte[] bytes = new byte[entry.getLength()];
      int result = inputStream.read(bytes);
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
//      outputStream.write(output.getBytes(StandardCharsets.UTF_8), currentOffset, output.length());
      outputStream.write(value.getBytes(StandardCharsets.UTF_8));
      outputStream.flush();

      if (offsetMap.containsKey(key)) {
        Entry entry = offsetMap.get(key);
        entry.setOffset(currentOffset);
        entry.setLength(value.length());
      } else {
        Entry entry = new Entry(currentOffset, value.length());
        offsetMap.put(key, entry);
      }

      System.out.printf((SAVED_LOG) + "%n", key, value, currentOffset);
      currentOffset += value.length();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

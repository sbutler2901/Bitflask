package dev.sbutler.bitflask.storage.lsm;

import java.util.Optional;

public class LSMTree {

  public Optional<String> read(String key) {
    // check memtable
    // check segments
    return Optional.empty();
  }

  public void write(String key, String value) {
    // make entry
    // write to WriteAheadLog
    // write to Memtable
  }

  public void delete(String key) {
    // make entry
    // use write()
  }
}

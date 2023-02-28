package dev.sbutler.bitflask.storage.segment;

record SegmentIndexEntry(String key, long offset) {

  static SegmentIndexEntry fromBytes(byte[] bytes) {
    // TODO: implement
    return null;
  }

  byte[] getBytes() {
    // TODO: implement
    return new byte[0];
  }
}

package dev.sbutler.bitflask.storage.segment;

class SegmentLoaderException extends RuntimeException {

  SegmentLoaderException(String message) {
    super(message);
  }

  SegmentLoaderException(String message, Throwable cause) {
    super(message, cause);
  }
}

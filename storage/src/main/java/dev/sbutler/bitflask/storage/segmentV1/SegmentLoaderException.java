package dev.sbutler.bitflask.storage.segmentV1;

class SegmentLoaderException extends RuntimeException {

  SegmentLoaderException(String message) {
    super(message);
  }

  SegmentLoaderException(String message, Throwable cause) {
    super(message, cause);
  }
}

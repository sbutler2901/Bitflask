package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import java.util.Deque;
import javax.inject.Inject;
import org.slf4j.Logger;

class SegmentLoaderImpl implements SegmentLoader {

  @InjectStorageLogger
  Logger logger;

  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLoaderImpl(SegmentFactory segmentFactory) {
    this.segmentFactory = segmentFactory;
  }

  @Override
  public Deque<Segment> loadExistingSegments() {
    return null;
  }

}

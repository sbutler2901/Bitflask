package dev.sbutler.bitflask.storage.lsm.segment;

import static org.mockito.Mockito.mock;

import java.util.concurrent.ThreadFactory;

public class SegmentLevelCompactorTest {

  private final ThreadFactory threadFactory = Thread.ofVirtual().factory();
  private final SegmentFactory segmentFactory = mock(SegmentFactory.class);

  private final SegmentLevelCompactor compactor =
      new SegmentLevelCompactor(threadFactory, segmentFactory);
}

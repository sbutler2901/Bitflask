package dev.sbutler.bitflask.storage.segment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentLoaderImpl {

  @InjectMocks
  SegmentLoaderImpl segmentLoader;
  @Mock
  SegmentFactory segmentFactory;

  @Test
  void loadExistingSegments() {
  }
}

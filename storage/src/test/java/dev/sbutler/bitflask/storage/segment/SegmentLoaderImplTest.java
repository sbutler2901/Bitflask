package dev.sbutler.bitflask.storage.segment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// todo: implement tests
@ExtendWith(MockitoExtension.class)
class SegmentLoaderImplTest {

  @InjectMocks
  SegmentLoaderImplTest segmentLoader;
  @Mock
  SegmentFactory segmentFactory;

  @Test
  void loadExistingSegments() {
  }
}

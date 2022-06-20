package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentCompactorProviderTest {

  @InjectMocks
  SegmentCompactorProvider segmentCompactorProvider;
  @Mock
  ExecutorService executorService;
  @Mock
  SegmentFactory segmentFactory;

  @Test
  void get() {
    SegmentCompactor segmentCompactor0 = segmentCompactorProvider.get();
    SegmentCompactor segmentCompactor1 = segmentCompactorProvider.get();
    assertNotEquals(segmentCompactor0, segmentCompactor1);
  }

}

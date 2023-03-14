package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class SegmentLoaderTest {

  private final SegmentIndex index_0 = mock(SegmentIndex.class);
  private final SegmentIndex index_1 = mock(SegmentIndex.class);

  private static final Path PATH_0 = Path.of("/tmp/segment_0.seg");
  private static final Path PATH_1 = Path.of("/tmp/segment_1.seg");

  private final Segment segment_0 = mock(Segment.class);
  private final Segment segment_1 = mock(Segment.class);

  private final ImmutableMap<Integer, SegmentIndex> segmentNumberToIndexMap =
      ImmutableMap.of(0, index_0, 1, index_1);

  private final ThreadFactory threadFactory = Thread.ofVirtual().factory();
  private final StorageConfigurations configurations = mock(StorageConfigurations.class);
  private final SegmentFactory segmentFactory = mock(SegmentFactory.class);

  private final SegmentLoader loader =
      new SegmentLoader(threadFactory, configurations, segmentFactory);

  @Test
  public void loadWithIndexes_success() throws Exception {
    try (MockedStatic<LoaderUtils> loaderUtilsMockedStatic = mockStatic(LoaderUtils.class)) {
      loaderUtilsMockedStatic.when(() -> LoaderUtils.loadPathsInDirForGlob(any(), any()))
          .thenReturn(ImmutableList.of(PATH_0, PATH_1));
      when(segmentFactory.loadFromPath(PATH_0, segmentNumberToIndexMap)).thenReturn(segment_0);
      when(segmentFactory.loadFromPath(PATH_1, segmentNumberToIndexMap)).thenReturn(segment_1);

      ImmutableList<Segment> segments = loader.loadWithIndexes(segmentNumberToIndexMap);

      assertThat(segments).containsExactly(segment_0, segment_1);
    }
  }

  @Test
  public void loadWithIndexes_indexFactoryThrowsIoException_throwsStorageLoadException()
      throws Exception {
    try (MockedStatic<LoaderUtils> loaderUtilsMockedStatic = mockStatic(LoaderUtils.class)) {
      loaderUtilsMockedStatic.when(() -> LoaderUtils.loadPathsInDirForGlob(any(), any()))
          .thenReturn(ImmutableList.of(PATH_0));
      IOException ioException = new IOException("test");
      when(segmentFactory.loadFromPath(PATH_0, segmentNumberToIndexMap)).thenThrow(ioException);

      StorageLoadException e = assertThrows(StorageLoadException.class,
          () -> loader.loadWithIndexes(segmentNumberToIndexMap));

      assertThat(e).hasCauseThat().isEqualTo(ioException);
      assertThat(e).hasMessageThat().isEqualTo("Failed loading Segments");
    }
  }
}

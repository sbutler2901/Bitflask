package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import dev.sbutler.bitflask.storage.lsm.utils.LoaderUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ThreadFactory;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class SegmentIndexLoaderTest {

  private static final Path PATH_0 = Path.of("/tmp/index_0.idx");
  private static final Path PATH_1 = Path.of("/tmp/index_1.idx");

  private final SegmentIndex index_0 = mock(SegmentIndex.class);
  private final SegmentIndex index_1 = mock(SegmentIndex.class);

  private final ThreadFactory threadFactory = Thread.ofVirtual().factory();
  private final StorageConfigurations configurations = mock(StorageConfigurations.class);
  private final SegmentIndexFactory segmentIndexFactory = mock(SegmentIndexFactory.class);

  private final SegmentIndexLoader loader =
      new SegmentIndexLoader(threadFactory, configurations, segmentIndexFactory);

  @Test
  public void load_success() throws Exception {
    try (MockedStatic<LoaderUtils> loaderUtilsMockedStatic = mockStatic(LoaderUtils.class)) {
      loaderUtilsMockedStatic.when(() -> LoaderUtils.loadPathsInDirForGlob(any(), any()))
          .thenReturn(ImmutableList.of(PATH_0, PATH_1));
      when(segmentIndexFactory.loadFromPath(PATH_0)).thenReturn(index_0);
      when(segmentIndexFactory.loadFromPath(PATH_1)).thenReturn(index_1);

      ImmutableList<SegmentIndex> indexes = loader.load();

      assertThat(indexes).containsExactly(index_0, index_1);
    }
  }

  @Test
  public void load_indexFactoryThrowsIoException_throwsStorageLoadException() throws Exception {
    try (MockedStatic<LoaderUtils> loaderUtilsMockedStatic = mockStatic(LoaderUtils.class)) {
      loaderUtilsMockedStatic.when(() -> LoaderUtils.loadPathsInDirForGlob(any(), any()))
          .thenReturn(ImmutableList.of(PATH_0));
      IOException ioException = new IOException("test");
      when(segmentIndexFactory.loadFromPath(PATH_0)).thenThrow(ioException);

      StorageLoadException e = assertThrows(StorageLoadException.class, loader::load);

      assertThat(e).hasCauseThat().isEqualTo(ioException);
      assertThat(e).hasMessageThat().isEqualTo("Failed loading SegmentIndexes");
    }
  }

  @Test
  public void truncate() {
    try (MockedStatic<LoaderUtils> loaderUtilsMockedStatic = mockStatic(LoaderUtils.class)) {
      loader.truncate();

      loaderUtilsMockedStatic.verify(
          () -> LoaderUtils.deletePathsInDirForGlob(any(), any()),
          times(1));
    }
  }
}

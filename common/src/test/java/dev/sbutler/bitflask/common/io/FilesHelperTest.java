package dev.sbutler.bitflask.common.io;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FilesHelperTest {

  FilesHelper filesHelper;
  ThreadFactory threadFactory;

  @BeforeEach
  void beforeEach() {
    threadFactory = new MockThreadFactory();
    filesHelper = new FilesHelper(threadFactory);
  }

  @Test
  public void getLastModifiedTimeOfFiles() throws Exception {
    // Arrange
    ImmutableList<Path> paths = ImmutableList.of(Path.of("path0.txt"), Path.of("path1.txt"));
    // Act
    ImmutableMap<Path, Future<FileTime>> pathFileTimeFutures = filesHelper.getLastModifiedTimeOfFiles(
        paths);
    // Assert
    assertThat(pathFileTimeFutures).hasSize(2);
    assertThat(pathFileTimeFutures).containsKey(paths.get(0));
    assertThat(pathFileTimeFutures).containsKey(paths.get(1));
  }

  @Test
  public void openFileChannels() throws Exception {
    // Arrange
    ImmutableList<Path> paths = ImmutableList.of(Path.of("path0.txt"), Path.of("path1.txt"));
    ImmutableSet<StandardOpenOption> openOptions =
        ImmutableSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    // Act
    ImmutableMap<Path, Future<FileChannel>> pathFileTimeFutures =
        filesHelper.openFileChannels(paths, openOptions);
    // Assert
    assertThat(pathFileTimeFutures).hasSize(2);
    assertThat(pathFileTimeFutures).containsKey(paths.get(0));
    assertThat(pathFileTimeFutures).containsKey(paths.get(1));
  }

  @Test
  public void closeFileChannelsBestEffort() throws IOException {
    // Arrange
    FileChannel fc0 = mock(FileChannel.class);
    FileChannel fc1 = mock(FileChannel.class);
    doThrow(IOException.class).when(fc1).close();
    ImmutableList<FileChannel> fileChannels = ImmutableList.of(fc0, fc1);
    // Act
    filesHelper.closeFileChannelsBestEffort(fileChannels);
    // Assert
    verify(fc0, times(1)).close();
    verify(fc1, times(1)).close();
  }

  /**
   * Mocking {@link FileChannel#open(Path, Set, FileAttribute[])} in a callable does not appear to
   * work. This is a solution around it to prevent any callables from actually executing.
   */
  private static final class MockThreadFactory implements ThreadFactory {

    @Override
    public Thread newThread(@Nonnull Runnable r) {
      return new Thread(() -> {
      });
    }
  }
}

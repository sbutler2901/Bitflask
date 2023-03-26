package dev.sbutler.bitflask.storage.lsm.utils;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.exceptions.StorageLoadException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for loaders.
 */
public class LoaderUtils {

  /**
   * Loads the paths of all files in the directory that match the glob.
   */
  public static ImmutableList<Path> loadPathsInDirForGlob(Path dirPath, String glob) {
    if (!Files.exists(dirPath)) {
      throw new StorageLoadException(String.format("Directory does not exist [%s]", dirPath));
    }

    ImmutableList.Builder<Path> paths = ImmutableList.builder();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, glob)) {
      for (var path : stream) {
        paths.add(path);
      }
    } catch (IOException e) {
      throw new StorageLoadException(String.format(
          "Failed to load paths in storage dir [%s] for glob [%s]", dirPath, glob), e);
    }
    return paths.build();
  }

  /**
   * Deletes all files in the directory that match the glob.
   */
  public static void deletePathsInDirForGlob(Path dirPath, String glob) {
    ImmutableList<Path> paths = loadPathsInDirForGlob(dirPath, glob);
    for (var path : paths) {
      try {
        Files.delete(path);
      } catch (IOException e) {
        throw new StorageLoadException(String.format("Failed to delete [%s]", path), e);
      }
    }
  }

  /**
   * Checks that number of loaded bytes equals the expected, or throws a
   * {@link dev.sbutler.bitflask.storage.exceptions.StorageLoadException}.
   */
  public static <T> void checkLoadedBytesLength(
      byte[] loadedBytes,
      int expectedNumBytes,
      Class<T> clazz) {
    if (loadedBytes.length == expectedNumBytes) {
      return;
    }
    throw new StorageLoadException(
        String.format("%s bytes read too short. Expected [%d], actual [%d]",
            clazz.getSimpleName(), expectedNumBytes, loadedBytes.length));
  }

  private LoaderUtils() {
  }
}

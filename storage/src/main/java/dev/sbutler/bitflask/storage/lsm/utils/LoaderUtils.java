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
    ImmutableList.Builder<Path> paths = ImmutableList.builder();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, glob)) {
      stream.forEach(paths::add);
    } catch (IOException e) {
      throw new StorageLoadException(String.format(
          "Failed to load paths in storage dir [%s] for glob [%s]", dirPath, glob), e);
    }
    return paths.build();
  }

  private LoaderUtils() {
  }
}

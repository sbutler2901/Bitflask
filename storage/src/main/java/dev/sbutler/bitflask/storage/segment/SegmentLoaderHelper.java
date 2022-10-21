package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;
import javax.inject.Inject;

final class SegmentLoaderHelper {

  @Inject
  SegmentLoaderHelper() {

  }

  ImmutableList<Path> sortFilePathsByLatestModifiedDatesFirst(ImmutableList<Path> segmentFilePaths)
      throws IOException {
    // More recent modified first
    ImmutableSortedMap.Builder<FileTime, Path> pathFileTimeMapBuilder =
        new ImmutableSortedMap.Builder<>(Comparator.reverseOrder());
    for (Path path : segmentFilePaths) {
      FileTime pathFileTime = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS);
      pathFileTimeMapBuilder.put(pathFileTime, path);
    }
    return pathFileTimeMapBuilder.build().values().asList();
  }


  void closeFileChannels(ImmutableList<FileChannel> fileChannels) {
    fileChannels.forEach(fileChannel -> {
      try {
        fileChannel.close();
      } catch (IOException ignored) {
        // Best effort to close opened channels
      }
    });
  }

  void closeSegmentFiles(ImmutableList<SegmentFile> segmentFiles) {
    segmentFiles.forEach(SegmentFile::close);
  }
}

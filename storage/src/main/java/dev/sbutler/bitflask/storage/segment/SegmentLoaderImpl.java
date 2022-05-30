package dev.sbutler.bitflask.storage.segment;

import dev.sbutler.bitflask.storage.configuration.logging.InjectStorageLogger;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.inject.Inject;
import org.slf4j.Logger;

class SegmentLoaderImpl implements SegmentLoader {

  @InjectStorageLogger
  Logger logger;

  private final SegmentFactory segmentFactory;

  @Inject
  SegmentLoaderImpl(SegmentFactory segmentFactory) {
    this.segmentFactory = segmentFactory;
  }

  @Override
  public Deque<Segment> loadExistingSegments() throws IOException {
    Deque<Segment> segments = new ConcurrentLinkedDeque<>();
    List<Path> segmentFilePaths = getSegmentFilePaths();
    return segments;
  }

  private List<Path> getSegmentFilePaths() throws IOException {
    Path segmentStoreDirPath = segmentFactory.getSegmentStoreDirPath();
    List<Path> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(segmentStoreDirPath)) {
      for (Path entry : stream) {
        result.add(entry);
      }
    } catch (DirectoryIteratorException ex) {
      // I/O error encountered during the iteration, the cause is an IOException
      throw ex.getCause();
    }
    return result;
  }
}

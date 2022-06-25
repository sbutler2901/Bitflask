package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.inject.Inject;

class StorageImpl implements Storage {

  private static final String WRITE_ERR_BAD_KEY = "Error writing data, provided key was null, empty, or longer than 256 characters";
  private static final String WRITE_ERR_BAD_VALUE = "Error writing data, provided value was null, empty, or longer than 256 characters";
  private static final String READ_ERR_BAD_KEY = "Error reading data, provided key was null, empty, or longer than 256 characters";

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ExecutorService executorService;
  private final SegmentManager segmentManager;

  @Inject
  public StorageImpl(@StorageExecutorService ExecutorService executorService,
      SegmentManager segmentManager) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
  }

  public Future<Void> write(String key, String value) {
    validateWriteArgs(key, value);
    Callable<Void> writeTask = () -> {
      segmentManager.write(key, value);
      return null;
    };
    logger.atInfo().log("Submitting write for [%s] : [%s]", key, value);
    return executorService.submit(writeTask);
  }

  private void validateWriteArgs(String key, String value) {
    if (key == null || key.length() <= 0 || key.length() > 256) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_KEY);
    } else if (value == null || value.length() <= 0 || value.length() > 256) {
      throw new IllegalArgumentException(WRITE_ERR_BAD_VALUE);
    }
  }

  public Future<Optional<String>> read(String key) {
    validateReadArgs(key);
    Callable<Optional<String>> readTask = () -> segmentManager.read(key);
    logger.atInfo().log("Submitting read for [%s]", key);
    return executorService.submit(readTask);
  }

  private void validateReadArgs(String key) {
    if (key == null || key.length() <= 0 || key.length() > 256) {
      throw new IllegalArgumentException(READ_ERR_BAD_KEY);
    }
  }

}

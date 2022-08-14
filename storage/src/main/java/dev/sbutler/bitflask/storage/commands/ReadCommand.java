package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ReadCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentManager segmentManager;
  private final ReadDTO readDTO;

  public ReadCommand(ListeningExecutorService executorService, SegmentManager segmentManager,
      ReadDTO readDTO) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
    this.readDTO = readDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    String key = readDTO.key();
    Callable<StorageResponse> readTask = () -> {
      try {
        Optional<String> value = segmentManager.read(key);
        logger.atInfo().log("Successful read of [%s]:[%s]", key, value);
        if (value.isEmpty()) {
          value = Optional.of(String.format("[%s] not found", key));
        }
        return new Success(value.get());
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("Failed to read [%s]", key);
        return new Failed(String.format("Failure to read [%s]", key));
      }
    };
    logger.atInfo().log("Submitting read for [%s]", key);
    return Futures.submit(readTask, executorService);
  }
}

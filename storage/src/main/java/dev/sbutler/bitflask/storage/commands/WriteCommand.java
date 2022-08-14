package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import java.io.IOException;
import java.util.concurrent.Callable;

public class WriteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentManager segmentManager;
  private final WriteDTO writeDTO;

  public WriteCommand(ListeningExecutorService executorService, SegmentManager segmentManager,
      WriteDTO writeDTO) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
    this.writeDTO = writeDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    String key = writeDTO.key();
    String value = writeDTO.value();

    Callable<StorageResponse> writeTask = () -> {
      try {
        segmentManager.write(key, value);
        logger.atInfo().log("Successful write of [%s]:[%s]", key, value);
        return new Success("OK");
      } catch (IOException e) {
        logger.atWarning().withCause(e).log("Failed to write [%s]:[%s]", key, value);
        return new Failed(String.format("Failure to write [%s]:[%s]", key, value));
      }
    };

    logger.atInfo().log("Submitting write for [%s] : [%s]", key, value);
    return Futures.submit(writeTask, executorService);
  }
}

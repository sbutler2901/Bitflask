package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.Segment;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;

public class WriteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ManagedSegments managedSegments;
  private final WriteDTO writeDTO;

  public WriteCommand(ListeningExecutorService executorService, ManagedSegments managedSegments,
      WriteDTO writeDTO) {
    this.executorService = executorService;
    this.managedSegments = managedSegments;
    this.writeDTO = writeDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    logger.atInfo().log("Submitting write for [%s] : [%s]", writeDTO.key(), writeDTO.value());
    return Futures.submit(this::write, executorService);
  }

  private StorageResponse write() {
    String key = writeDTO.key();
    String value = writeDTO.value();
    Segment writableSegment = managedSegments.writableSegment();

    logger.atInfo().log("Writing [%s] : [%s] to segment [%d]", key, value,
        writableSegment.getSegmentFileKey());

    try {
      writableSegment.write(key, value);
      logger.atInfo().log("Successful write of [%s]:[%s]", key, value);
      return new Success("OK");
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to write [%s]:[%s]", key, value);
      return new Failed(String.format("Failure to write [%s]:[%s]", key, value));
    }
  }
}

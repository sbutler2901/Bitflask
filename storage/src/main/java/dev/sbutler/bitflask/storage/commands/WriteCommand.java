package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import dev.sbutler.bitflask.storage.segment.WritableSegment;
import java.io.IOException;

/**
 * Handles submitting an asynchronous task to the storage engine for writing a key:value mapping.
 */
public class WriteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final SegmentManagerService segmentManagerService;
  private final WriteDTO writeDTO;

  public WriteCommand(
      ListeningExecutorService listeningExecutorService,
      SegmentManagerService segmentManagerService,
      WriteDTO writeDTO) {
    this.listeningExecutorService = listeningExecutorService;
    this.segmentManagerService = segmentManagerService;
    this.writeDTO = writeDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    logger.atInfo().log("Submitting write for [%s] : [%s]", writeDTO.key(), writeDTO.value());
    return Futures.submit(this::write, listeningExecutorService);
  }

  private StorageResponse write() {
    String key = writeDTO.key();
    String value = writeDTO.value();
    WritableSegment segment = segmentManagerService.getWritableSegment();

    logger.atInfo().log("Writing [%s] : [%s] to segment [%d]", key, value,
        segment.getSegmentFileKey());

    try {
      segment.write(key, value);
      logger.atInfo().log("Successful write of [%s]:[%s]", key, value);
      return new Success("OK");
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to write [%s]:[%s]", key, value);
      return new Failed(String.format("Failure to write [%s]:[%s]", key, value));
    }
  }
}

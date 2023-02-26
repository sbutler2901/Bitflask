package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService;
import dev.sbutler.bitflask.storage.segmentV1.WritableSegment;
import java.io.IOException;

/**
 * Handles submitting an asynchronous task to the storage engine for deleting any mappings for the
 * provided key.
 */
public class DeleteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final SegmentManagerService segmentManagerService;
  private final DeleteDTO deleteDTO;

  public DeleteCommand(
      ListeningExecutorService listeningExecutorService,
      SegmentManagerService segmentManagerService,
      DeleteDTO deleteDTO) {
    this.listeningExecutorService = listeningExecutorService;
    this.segmentManagerService = segmentManagerService;
    this.deleteDTO = deleteDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    logger.atInfo().log("Submitting delete for [%s]", deleteDTO.key());
    return Futures.submit(this::delete, listeningExecutorService);
  }

  private StorageResponse delete() {
    String key = deleteDTO.key();
    try {
      WritableSegment segment = segmentManagerService.getWritableSegment();

      logger.atInfo().log("Deleting [%s] from segment [%d]", key,
          segment.getSegmentFileKey());

      segment.delete(key);
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to delete [%s]", key);
      return new Failed(String.format("Failure to delete [%s]", key));
    }
    return new Success("OK");
  }
}

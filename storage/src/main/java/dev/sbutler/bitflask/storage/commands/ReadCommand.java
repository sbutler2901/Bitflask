package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.Segment;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import java.io.IOException;
import java.util.Optional;

/**
 * Handles submitting an asynchronous task to the storage engine for reading the value of a provided
 * key.
 */
public class ReadCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ManagedSegments managedSegments;
  private final ReadDTO readDTO;

  public ReadCommand(ListeningExecutorService executorService, ManagedSegments managedSegments,
      ReadDTO readDTO) {
    this.executorService = executorService;
    this.managedSegments = managedSegments;
    this.readDTO = readDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    logger.atInfo().log("Submitting read for [%s]", readDTO.key());
    return Futures.submit(this::read, executorService);
  }

  private StorageResponse read() {
    return findLatestSegmentWithKey()
        .map(this::readFromSegment)
        .orElseGet(() -> {
          logger.atInfo().log("Could not find a segment containing key [%s]", readDTO.key());
          return notFoundResponse();
        });
  }

  private StorageResponse readFromSegment(Segment segment) {
    String key = readDTO.key();

    logger.atInfo()
        .log("Reading value of [%s] from segment [%d]", key,
            segment.getSegmentFileKey());

    try {
      return segment.read(key)
          .<StorageResponse>map(Success::new)
          .orElseGet(this::notFoundResponse);
    } catch (IOException e) {
      logger.atWarning().withCause(e).log("Failed to read [%s]", key);
      return new Failed(String.format("Failure to read [%s]", key));
    }
  }

  private StorageResponse notFoundResponse() {
    return new Success(String.format("[%s] not found", readDTO.key()));
  }

  private Optional<Segment> findLatestSegmentWithKey() {
    String key = readDTO.key();
    if (managedSegments.writableSegment().containsKey(key)) {
      return Optional.of(managedSegments.writableSegment());
    }
    for (Segment segment : managedSegments.frozenSegments()) {
      if (segment.containsKey(key)) {
        return Optional.of(segment);
      }
    }
    return Optional.empty();
  }
}

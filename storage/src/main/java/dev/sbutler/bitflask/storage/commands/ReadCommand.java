package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.ReadableSegment;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.io.IOException;
import java.util.Optional;

/**
 * Handles submitting an asynchronous task to the storage engine for reading the value of a provided
 * key.
 */
public class ReadCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final SegmentManagerService segmentManagerService;
  private final ReadDTO readDTO;

  public ReadCommand(
      ListeningExecutorService executorService,
      SegmentManagerService segmentManagerService,
      ReadDTO readDTO) {
    this.executorService = executorService;
    this.segmentManagerService = segmentManagerService;
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

  private StorageResponse readFromSegment(ReadableSegment segment) {
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

  private Optional<ReadableSegment> findLatestSegmentWithKey() {
    return segmentManagerService.getReadableSegments().stream()
        .filter(s -> s.containsKey(readDTO.key()))
        .findFirst();
  }
}

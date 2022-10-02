package dev.sbutler.bitflask.storage.commands;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;

public class DeleteCommand implements StorageCommand {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final ManagedSegments managedSegments;
  private final DeleteDTO deleteDTO;

  public DeleteCommand(
      ListeningExecutorService executorService,
      ManagedSegments managedSegments,
      DeleteDTO deleteDTO) {
    this.executorService = executorService;
    this.managedSegments = managedSegments;
    this.deleteDTO = deleteDTO;
  }

  @Override
  public ListenableFuture<StorageResponse> execute() {
    return null;
  }
}

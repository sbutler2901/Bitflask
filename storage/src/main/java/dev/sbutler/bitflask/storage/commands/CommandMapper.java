package dev.sbutler.bitflask.storage.commands;

import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService.ManagedSegments;
import javax.inject.Inject;

/**
 * Maps incoming {@link StorageCommandDTO}s into executable {@link StorageCommand}s.
 */
public class CommandMapper {

  private final ListeningExecutorService executorService;
  private final SegmentManagerService segmentManagerService;

  @Inject
  public CommandMapper(@StorageExecutorService ListeningExecutorService executorService,
      SegmentManagerService segmentManagerService) {
    this.executorService = executorService;
    this.segmentManagerService = segmentManagerService;
  }

  public StorageCommand mapToCommand(StorageCommandDTO commandDTO) {
    // TODO: provide segment manager service to commands
    ManagedSegments managedSegments = segmentManagerService.getManagedSegments();
    return switch (commandDTO) {
      case ReadDTO readDTO -> new ReadCommand(executorService, managedSegments, readDTO);
      case WriteDTO writeDTO -> new WriteCommand(executorService, managedSegments, writeDTO);
      case DeleteDTO deleteDTO -> new DeleteCommand(executorService, managedSegments, deleteDTO);
    };
  }
}

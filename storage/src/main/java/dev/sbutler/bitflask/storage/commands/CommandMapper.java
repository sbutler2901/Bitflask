package dev.sbutler.bitflask.storage.commands;

import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.configuration.concurrency.StorageExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.segment.SegmentManager;
import dev.sbutler.bitflask.storage.segment.SegmentManager.ManagedSegments;
import javax.inject.Inject;

public class CommandMapper {

  private final ListeningExecutorService executorService;
  private final SegmentManager segmentManager;

  @Inject
  public CommandMapper(@StorageExecutorService ListeningExecutorService executorService,
      SegmentManager segmentManager) {
    this.executorService = executorService;
    this.segmentManager = segmentManager;
  }

  public StorageCommand mapToCommand(StorageCommandDTO commandDTO) {
    ManagedSegments managedSegments = segmentManager.getManagedSegments();
    return switch (commandDTO) {
      case ReadDTO readDTO -> new ReadCommand(executorService, managedSegments, readDTO);
      case WriteDTO writeDTO -> new WriteCommand(executorService, managedSegments, writeDTO);
    };
  }
}

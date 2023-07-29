package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Manages persisting and retrieving data. */
@Singleton
public final class StorageService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final CommandMapper commandMapper;
  private final StorageLoader storageLoader;

  @Inject
  StorageService(LSMTree lsmTree, CommandMapper commandMapper, StorageLoader storageLoader) {
    this.lsmTree = lsmTree;
    this.commandMapper = commandMapper;
    this.storageLoader = storageLoader;
  }

  @Override
  protected void startUp() {
    storageLoader.load();
    logger.atInfo().log("storage loaded.");
  }

  public StorageResponse processCommand(StorageCommandDTO commandDTO) {
    StorageCommand command = commandMapper.mapToCommand(commandDTO);
    return command.execute();
  }

  @Override
  protected void shutDown() {
    lsmTree.close();
    logger.atInfo().log("storage shutdown");
  }
}

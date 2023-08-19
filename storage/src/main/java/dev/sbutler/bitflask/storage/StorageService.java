package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.commands.StorageCommandFactory;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import dev.sbutler.bitflask.storage.raft.Raft;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Manages persisting and retrieving data. */
@Singleton
public final class StorageService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final Raft raft;
  private final StorageCommandFactory storageCommandFactory;
  private final StorageLoader storageLoader;

  @Inject
  StorageService(
      LSMTree lsmTree,
      Raft raft,
      StorageCommandFactory storageCommandFactory,
      StorageLoader storageLoader) {
    this.lsmTree = lsmTree;
    this.raft = raft;
    this.storageCommandFactory = storageCommandFactory;
    this.storageLoader = storageLoader;
  }

  @Override
  protected void startUp() {
    storageLoader.load();
    logger.atInfo().log("storage loaded.");
  }

  public StorageSubmitResults processCommand(StorageCommandDTO commandDTO) {
    StorageCommand storageCommand = storageCommandFactory.createStorageCommand(commandDTO);
    ClientCommand command = new ClientCommand(raft, storageCommand);
    return command.execute();
  }

  @Override
  protected void shutDown() {
    lsmTree.close();
    logger.atInfo().log("storage shutdown");
  }
}

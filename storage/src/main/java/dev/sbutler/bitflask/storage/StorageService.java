package dev.sbutler.bitflask.storage;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/** Manages persisting and retrieving data. */
@Singleton
public final class StorageService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final LSMTree lsmTree;
  private final ClientCommandFactory clientCommandFactory;
  private final StorageLoader storageLoader;

  @Inject
  StorageService(
      LSMTree lsmTree, ClientCommandFactory clientCommandFactory, StorageLoader storageLoader) {
    this.lsmTree = lsmTree;
    this.clientCommandFactory = clientCommandFactory;
    this.storageLoader = storageLoader;
  }

  @Override
  protected void startUp() {
    storageLoader.load();
    logger.atInfo().log("storage loaded.");
  }

  public StorageSubmitResults processCommand(StorageCommandDTO commandDTO) {
    ClientCommand command = clientCommandFactory.createClientCommand(commandDTO);
    return command.execute();
  }

  @Override
  protected void shutDown() {
    lsmTree.close();
    logger.atInfo().log("storage shutdown");
  }
}

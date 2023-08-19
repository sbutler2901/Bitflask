package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO;

/** A {@link ServerCommand} that interacts with storage. */
public class ServerStorageCommand implements ServerCommand {

  private final ClientCommandFactory clientCommandFactory;
  private final StorageCommandDTO storageCommandDTO;

  ServerStorageCommand(
      ClientCommandFactory clientCommandFactory, StorageCommandDTO storageCommandDTO) {
    this.clientCommandFactory = clientCommandFactory;
    this.storageCommandDTO = storageCommandDTO;
  }

  @Override
  public ClientCommandResults execute() {
    ClientCommand clientCommand = clientCommandFactory.create(storageCommandDTO);
    return clientCommand.execute();
  }
}

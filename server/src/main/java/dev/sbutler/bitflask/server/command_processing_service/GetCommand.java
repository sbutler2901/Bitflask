package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.ReadDTO;

/** Submits a blocking read request to the storage engine and processes the results. */
class GetCommand implements ServerCommand {

  private final ClientCommandFactory clientCommandFactory;
  private final String key;

  public GetCommand(ClientCommandFactory clientCommandFactory, String key) {
    this.clientCommandFactory = clientCommandFactory;
    this.key = key;
  }

  @Override
  public ClientCommandResults execute() {
    StorageCommandDTO storageCommandDTO = new ReadDTO(key);
    ClientCommand command = clientCommandFactory.create(storageCommandDTO);
    return command.execute();
  }
}

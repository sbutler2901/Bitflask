package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.DeleteDTO;

/** Submits a blocking delete request to the storage engine and processes the results. */
public class DeleteCommand implements ServerCommand {

  private final ClientCommandFactory clientCommandFactory;
  private final String key;

  public DeleteCommand(ClientCommandFactory clientCommandFactory, String key) {
    this.clientCommandFactory = clientCommandFactory;
    this.key = key;
  }

  @Override
  public ClientCommandResults execute() {
    StorageCommandDTO storageCommandDTO = new DeleteDTO(key);
    ClientCommand command = clientCommandFactory.create(storageCommandDTO);
    return command.execute();
  }
}

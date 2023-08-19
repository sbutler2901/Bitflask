package dev.sbutler.bitflask.server.command_processing_service;

import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import dev.sbutler.bitflask.storage.commands.ClientCommandResults;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.WriteDTO;

/** Submits a blocking write request to the storage engine and processes the results. */
class SetCommand implements ServerCommand {

  private final ClientCommandFactory clientCommandFactory;
  private final String key;
  private final String value;

  public SetCommand(ClientCommandFactory clientCommandFactory, String key, String value) {
    this.clientCommandFactory = clientCommandFactory;
    this.key = key;
    this.value = value;
  }

  @Override
  public ClientCommandResults execute() {
    StorageCommandDTO storageCommandDTO = new WriteDTO(key, value);
    ClientCommand command = clientCommandFactory.create(storageCommandDTO);
    return command.execute();
  }
}

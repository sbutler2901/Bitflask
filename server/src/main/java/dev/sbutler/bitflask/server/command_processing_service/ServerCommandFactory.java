package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO;
import jakarta.inject.Inject;
import java.util.List;

/** Handles creating {@link ServerCommand}s. */
final class ServerCommandFactory {

  private final ClientCommandFactory clientCommandFactory;

  @Inject
  ServerCommandFactory(ClientCommandFactory clientCommandFactory) {
    this.clientCommandFactory = clientCommandFactory;
  }

  /** */
  ServerCommand createCommand(ServerCommandType serverCommandType, ImmutableList<String> args) {
    if (!isValidCommandArgs(serverCommandType, args)) {
      throw new InvalidCommandException(
          String.format("Invalid arguments for command [%s]: %s", serverCommandType, args));
    }

    return switch (serverCommandType) {
      case PING -> new ServerPingCommand();
      case GET -> {
        var storageCommandDTO = new StorageCommandDTO.ReadDTO(args.get(0));
        yield new ServerStorageCommand(clientCommandFactory, storageCommandDTO);
      }
      case SET -> {
        var storageCommandDTO = new StorageCommandDTO.WriteDTO(args.get(0), args.get(1));
        yield new ServerStorageCommand(clientCommandFactory, storageCommandDTO);
      }
      case DEL -> {
        var storageCommandDTO = new StorageCommandDTO.DeleteDTO(args.get(0));
        yield new ServerStorageCommand(clientCommandFactory, storageCommandDTO);
      }
    };
  }

  private static boolean isValidCommandArgs(
      ServerCommandType serverCommandType, List<String> args) {
    return switch (serverCommandType) {
      case PING -> args.isEmpty();
      case GET, DEL -> args.size() == 1;
      case SET -> args.size() == 2;
    };
  }
}

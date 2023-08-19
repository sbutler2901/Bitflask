package dev.sbutler.bitflask.server.command_processing_service;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.storage.commands.ClientCommandFactory;
import jakarta.inject.Inject;
import java.util.List;

/** Handles creating {@link ServerCommand}s. */
final class CommandFactory {

  private final ClientCommandFactory clientCommandFactory;

  @Inject
  CommandFactory(ClientCommandFactory clientCommandFactory) {
    this.clientCommandFactory = clientCommandFactory;
  }

  /** */
  ServerCommand createCommand(CommandType commandType, ImmutableList<String> args) {
    if (!isValidCommandArgs(commandType, args)) {
      throw new InvalidCommandException(
          String.format("Invalid arguments for command [%s]: %s", commandType, args));
    }

    return switch (commandType) {
      case PING -> createPingCommand();
      case GET -> createGetCommand(args.get(0));
      case SET -> createSetCommand(args.get(0), args.get(1));
      case DEL -> createDeleteCommand(args.get(0));
    };
  }

  private PingCommand createPingCommand() {
    return new PingCommand();
  }

  private GetCommand createGetCommand(String key) {
    return new GetCommand(clientCommandFactory, key);
  }

  private SetCommand createSetCommand(String key, String value) {
    return new SetCommand(clientCommandFactory, key, value);
  }

  private DeleteCommand createDeleteCommand(String key) {
    return new DeleteCommand(clientCommandFactory, key);
  }

  private static boolean isValidCommandArgs(CommandType commandType, List<String> args) {
    return switch (commandType) {
      case PING -> args.isEmpty();
      case GET, DEL -> args.size() == 1;
      case SET -> args.size() == 2;
    };
  }
}

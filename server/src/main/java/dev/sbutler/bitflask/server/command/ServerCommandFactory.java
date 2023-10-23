package dev.sbutler.bitflask.server.command;

import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import jakarta.inject.Inject;

/** Handles creating {@link ServerCommand}s from client requests. */
public final class ServerCommandFactory {

  private final ClientCommand.Factory clientCommandFactory;

  @Inject
  ServerCommandFactory(ClientCommand.Factory clientCommandFactory) {
    this.clientCommandFactory = clientCommandFactory;
  }

  /** Creates a ServerCommand from a {@link RespRequest}. */
  public ServerCommand createCommand(RespRequest request) {
    return switch (request) {
      case RespRequest.PingRequest _ignored -> new ServerPingCommand();
      case RespRequest.GetRequest getRequest -> {
        var storageCommandDTO = new StorageCommandDto.ReadDto(getRequest.getKey());
        yield new ServerStorageCommand(clientCommandFactory.create(storageCommandDTO));
      }
      case RespRequest.SetRequest setRequest -> {
        var storageCommandDTO =
            new StorageCommandDto.WriteDto(setRequest.getKey(), setRequest.getValue());
        yield new ServerStorageCommand(clientCommandFactory.create(storageCommandDTO));
      }
      case RespRequest.DeleteRequest deleteRequest -> {
        var storageCommandDTO = new StorageCommandDto.DeleteDto(deleteRequest.getKey());
        yield new ServerStorageCommand(clientCommandFactory.create(storageCommandDTO));
      }
    };
  }
}

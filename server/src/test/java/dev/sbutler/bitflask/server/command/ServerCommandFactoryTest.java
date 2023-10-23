package dev.sbutler.bitflask.server.command;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.resp.messages.RespRequest;
import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Unit tests for {@link ServerCommandFactory}. */
public class ServerCommandFactoryTest {

  private final ClientCommand.Factory clientCommandFactory = mock(ClientCommand.Factory.class);

  private final ServerCommandFactory serverCommandFactory =
      new ServerCommandFactory(clientCommandFactory);

  @Test
  public void createCommand_respRequest_ping() {
    RespRequest request = new RespRequest.PingRequest();

    ServerCommand serverCommand = serverCommandFactory.createCommand(request);

    assertThat(serverCommand).isInstanceOf(ServerPingCommand.class);
  }

  @Test
  public void createCommand_respRequest_get() {
    RespRequest request = new RespRequest.GetRequest("key");

    ServerCommand serverCommand = serverCommandFactory.createCommand(request);

    assertThat(serverCommand).isInstanceOf(ServerStorageCommand.class);
    ArgumentCaptor<StorageCommandDto.ReadDto> commandDtoCaptor =
        ArgumentCaptor.forClass(StorageCommandDto.ReadDto.class);
    verify(clientCommandFactory, times(1)).create(commandDtoCaptor.capture());
    assertThat(commandDtoCaptor.getValue().key()).isEqualTo("key");
  }

  @Test
  public void createCommand_respRequest_set() {
    RespRequest request = new RespRequest.SetRequest("key", "value");

    ServerCommand serverCommand = serverCommandFactory.createCommand(request);

    assertThat(serverCommand).isInstanceOf(ServerStorageCommand.class);
    ArgumentCaptor<StorageCommandDto.WriteDto> commandDtoCaptor =
        ArgumentCaptor.forClass(StorageCommandDto.WriteDto.class);
    verify(clientCommandFactory, times(1)).create(commandDtoCaptor.capture());
    assertThat(commandDtoCaptor.getValue().key()).isEqualTo("key");
    assertThat(commandDtoCaptor.getValue().value()).isEqualTo("value");
  }

  @Test
  public void createCommand_respRequest_delete() {
    RespRequest request = new RespRequest.DeleteRequest("key");

    ServerCommand serverCommand = serverCommandFactory.createCommand(request);

    assertThat(serverCommand).isInstanceOf(ServerStorageCommand.class);
    ArgumentCaptor<StorageCommandDto.DeleteDto> commandDtoCaptor =
        ArgumentCaptor.forClass(StorageCommandDto.DeleteDto.class);
    verify(clientCommandFactory, times(1)).create(commandDtoCaptor.capture());
    assertThat(commandDtoCaptor.getValue().key()).isEqualTo("key");
  }
}

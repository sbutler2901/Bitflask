package dev.sbutler.bitflask.storage.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.commands.*;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.integration.extensions.ListeningExecutorServiceExtension;
import dev.sbutler.bitflask.storage.integration.extensions.StorageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ListeningExecutorServiceExtension.class, StorageExtension.class})
public class StorageTest {

  private final ClientCommand.Factory clientCommandFactory;
  private final ListeningExecutorService listeningExecutorService;

  public StorageTest(
      ClientCommand.Factory clientCommandFactory,
      ListeningExecutorService listeningExecutorService) {
    this.clientCommandFactory = clientCommandFactory;
    this.listeningExecutorService = listeningExecutorService;
  }

  @Test
  public void write() throws Exception {
    StorageCommandDTO dto = new WriteDTO("key", "value");
    ClientCommand command = clientCommandFactory.create(dto);

    ClientCommandResults.Success response = getResponseAsSuccess(command.execute());

    assertThat(response.message()).isEqualTo("OK");
  }

  @Test
  public void read_notFound() {
    StorageCommandDTO dto = new ReadDTO("unknownKey");
    ClientCommand command = clientCommandFactory.create(dto);

    ClientCommandResults.Success response = getResponseAsSuccess(command.execute());

    assertThat(response.message()).isEqualTo("[unknownKey] not found");
  }

  @Test
  public void read_found() {
    StorageCommandDTO writeDto = new WriteDTO("key", "value");
    ClientCommand writeCommand = clientCommandFactory.create(writeDto);
    StorageCommandDTO readDto = new ReadDTO("key");
    ClientCommand readCommand = clientCommandFactory.create(readDto);

    ClientCommandResults.Success writeResponse = getResponseAsSuccess(writeCommand.execute());
    ClientCommandResults.Success readResponse = getResponseAsSuccess(readCommand.execute());

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("value");
  }

  @Test
  public void delete() {
    StorageCommandDTO writeDto = new WriteDTO("key", "value");
    ClientCommand writeCommand = clientCommandFactory.create(writeDto);
    StorageCommandDTO deleteDto = new DeleteDTO("key");
    ClientCommand deleteCommand = clientCommandFactory.create(deleteDto);
    StorageCommandDTO readDto = new ReadDTO("key");
    ClientCommand readCommand = clientCommandFactory.create(readDto);

    ClientCommandResults.Success writeResponse = getResponseAsSuccess(writeCommand.execute());
    ClientCommandResults.Success deleteResponse = getResponseAsSuccess(deleteCommand.execute());
    ClientCommandResults.Success readResponse = getResponseAsSuccess(readCommand.execute());

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(deleteResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("[key] not found");
  }

  @Test
  public void compactionStress() {
    var dtos =
        ImmutableList.<StorageCommandDTO>builder()
            .addAll(generateWriteCommands(50))
            .addAll(generateDeleteCommands(25))
            .addAll(generateReadCommands(50))
            .build();

    var responseFutures =
        dtos.stream()
            .map(clientCommandFactory::create)
            .map(command -> Futures.submit(command::execute, listeningExecutorService))
            .collect(toImmutableList());

    Futures.whenAllSucceed(responseFutures)
        .run(
            () ->
                responseFutures.stream()
                    .map(Futures::getUnchecked)
                    .forEach(
                        response ->
                            assertThat(response).isInstanceOf(StorageCommandResults.Success.class)),
            listeningExecutorService);
  }

  private static ClientCommandResults.Success getResponseAsSuccess(ClientCommandResults response) {
    assertThat(response).isInstanceOf(ClientCommandResults.Success.class);
    return (ClientCommandResults.Success) response;
  }

  private static ImmutableList<StorageCommandDTO.WriteDTO> generateWriteCommands(int num) {
    ImmutableList.Builder<StorageCommandDTO.WriteDTO> commands = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      commands.add(new WriteDTO("key_" + i, "value_" + i));
    }
    return commands.build();
  }

  private static ImmutableList<StorageCommandDTO.DeleteDTO> generateDeleteCommands(int num) {
    ImmutableList.Builder<StorageCommandDTO.DeleteDTO> commands = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      commands.add(new DeleteDTO("key_" + i));
    }
    return commands.build();
  }

  private static ImmutableList<StorageCommandDTO.ReadDTO> generateReadCommands(int num) {
    ImmutableList.Builder<StorageCommandDTO.ReadDTO> commands = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      commands.add(new ReadDTO("key_" + i));
    }
    return commands.build();
  }
}

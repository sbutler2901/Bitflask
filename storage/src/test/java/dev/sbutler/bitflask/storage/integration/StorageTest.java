package dev.sbutler.bitflask.storage.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.commands.*;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto.DeleteDto;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto.ReadDto;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto.WriteDto;
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
    StorageCommandDto dto = new WriteDto("key", "value");
    ClientCommand command = clientCommandFactory.create(dto);

    ClientCommandResults.Success response = getResponseAsSuccess(command.execute());

    assertThat(response.message()).isEqualTo("OK");
  }

  @Test
  public void read_notFound() {
    StorageCommandDto dto = new ReadDto("unknownKey");
    ClientCommand command = clientCommandFactory.create(dto);

    ClientCommandResults.Success response = getResponseAsSuccess(command.execute());

    assertThat(response.message()).isEqualTo("[unknownKey] not found");
  }

  @Test
  public void read_found() {
    StorageCommandDto writeDto = new WriteDto("key", "value");
    ClientCommand writeCommand = clientCommandFactory.create(writeDto);
    StorageCommandDto readDto = new ReadDto("key");
    ClientCommand readCommand = clientCommandFactory.create(readDto);

    ClientCommandResults.Success writeResponse = getResponseAsSuccess(writeCommand.execute());
    ClientCommandResults.Success readResponse = getResponseAsSuccess(readCommand.execute());

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("value");
  }

  @Test
  public void delete() {
    StorageCommandDto writeDto = new WriteDto("key", "value");
    ClientCommand writeCommand = clientCommandFactory.create(writeDto);
    StorageCommandDto deleteDto = new DeleteDto("key");
    ClientCommand deleteCommand = clientCommandFactory.create(deleteDto);
    StorageCommandDto readDto = new ReadDto("key");
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
        ImmutableList.<StorageCommandDto>builder()
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

  private static ImmutableList<WriteDto> generateWriteCommands(int num) {
    ImmutableList.Builder<WriteDto> commands = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      commands.add(new WriteDto("key_" + i, "value_" + i));
    }
    return commands.build();
  }

  private static ImmutableList<DeleteDto> generateDeleteCommands(int num) {
    ImmutableList.Builder<DeleteDto> commands = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      commands.add(new DeleteDto("key_" + i));
    }
    return commands.build();
  }

  private static ImmutableList<ReadDto> generateReadCommands(int num) {
    ImmutableList.Builder<ReadDto> commands = ImmutableList.builder();
    for (int i = 0; i < num; i++) {
      commands.add(new ReadDto("key_" + i));
    }
    return commands.build();
  }
}

package dev.sbutler.bitflask.storage.integration;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.StorageResponse;
import dev.sbutler.bitflask.storage.StorageService;
import dev.sbutler.bitflask.storage.integration.extensions.ListeningExecutorServiceExtension;
import dev.sbutler.bitflask.storage.integration.extensions.StorageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ListeningExecutorServiceExtension.class, StorageExtension.class})
public class StorageTest {

  private final StorageService storageService;
  private final ListeningExecutorService listeningExecutorService;

  public StorageTest(
      StorageService storageService, ListeningExecutorService listeningExecutorService) {
    this.storageService = storageService;
    this.listeningExecutorService = listeningExecutorService;
  }

  @Test
  public void write() {
    StorageCommandDTO dto = new WriteDTO("key", "value");

    StorageResponse.Success response = getResponseAsSuccess(storageService.processCommand(dto));

    assertThat(response.message()).isEqualTo("OK");
  }

  @Test
  public void read_notFound() {
    StorageCommandDTO dto = new ReadDTO("unknownKey");

    StorageResponse.Success response = getResponseAsSuccess(storageService.processCommand(dto));

    assertThat(response.message()).isEqualTo("[unknownKey] not found");
  }

  @Test
  public void read_found() {
    StorageCommandDTO writeDto = new WriteDTO("key", "value");
    StorageCommandDTO readDto = new ReadDTO("key");

    StorageResponse.Success writeResponse =
        getResponseAsSuccess(storageService.processCommand(writeDto));
    StorageResponse.Success readResponse =
        getResponseAsSuccess(storageService.processCommand(readDto));

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("value");
  }

  @Test
  public void delete() {
    StorageCommandDTO writeDto = new WriteDTO("key", "value");
    StorageCommandDTO deleteDto = new DeleteDTO("key");
    StorageCommandDTO readDto = new ReadDTO("key");

    StorageResponse.Success writeResponse =
        getResponseAsSuccess(storageService.processCommand(writeDto));
    StorageResponse.Success deleteResponse =
        getResponseAsSuccess(storageService.processCommand(deleteDto));
    StorageResponse.Success readResponse =
        getResponseAsSuccess(storageService.processCommand(readDto));

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(deleteResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("[key] not found");
  }

  @Test
  public void compactionStress() {
    var commands =
        ImmutableList.<StorageCommandDTO>builder()
            .addAll(generateWriteCommands(50))
            .addAll(generateDeleteCommands(25))
            .addAll(generateReadCommands(50))
            .build();

    var responseFutures =
        commands.stream()
            .map(
                dto ->
                    Futures.submit(
                        () -> storageService.processCommand(dto), listeningExecutorService))
            .collect(toImmutableList());

    Futures.whenAllSucceed(responseFutures)
        .run(
            () ->
                responseFutures.stream()
                    .map(Futures::getUnchecked)
                    .forEach(
                        response ->
                            assertThat(response).isInstanceOf(StorageResponse.Success.class)),
            listeningExecutorService);
  }

  private static StorageResponse.Success getResponseAsSuccess(StorageResponse response) {
    assertThat(response).isInstanceOf(StorageResponse.Success.class);
    return (StorageResponse.Success) response;
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

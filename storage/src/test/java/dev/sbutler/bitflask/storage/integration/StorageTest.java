package dev.sbutler.bitflask.storage.integration;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.integration.extensions.ListeningExecutorServiceExtension;
import dev.sbutler.bitflask.storage.integration.extensions.StorageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({ListeningExecutorServiceExtension.class, StorageExtension.class})
public class StorageTest {

  private final StorageCommandDispatcherHelper dispatcherHelper;

  public StorageTest(StorageCommandDispatcher commandDispatcher,
      ListeningExecutorService listeningExecutorService) {
    this.dispatcherHelper = new StorageCommandDispatcherHelper(commandDispatcher,
        listeningExecutorService);
  }

  @Test
  public void write() throws Exception {
    var responseFuture = dispatcherHelper.submitStorageCommand(new WriteDTO("key", "value"));

    StorageResponse.Success response = dispatcherHelper.getResponseAsSuccess(responseFuture);

    assertThat(response.message()).isEqualTo("OK");
  }

  @Test
  public void read_notFound() throws Exception {
    var responseFuture = dispatcherHelper.submitStorageCommand(new ReadDTO("unknownKey"));

    StorageResponse.Success response = dispatcherHelper.getResponseAsSuccess(responseFuture);

    assertThat(response.message()).isEqualTo("[unknownKey] not found");
  }

  @Test
  public void read_found() throws Exception {
    var responseFutures =
        dispatcherHelper.combineResponseFutures(ImmutableList.of(
            dispatcherHelper.submitStorageCommand(new WriteDTO("key", "value")),
            dispatcherHelper.submitStorageCommand(new ReadDTO("key"))));

    var responses = responseFutures.get();
    StorageResponse.Success writeResponse = dispatcherHelper.getResponseAsSuccess(responses.get(0));
    StorageResponse.Success readResponse = dispatcherHelper.getResponseAsSuccess(responses.get(1));

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("value");
  }

  @Test
  public void delete() throws Exception {
    var responseFutures =
        dispatcherHelper.combineResponseFutures(ImmutableList.of(
            dispatcherHelper.submitStorageCommand(new WriteDTO("key", "value")),
            dispatcherHelper.submitStorageCommand(new DeleteDTO("key")),
            dispatcherHelper.submitStorageCommand(new ReadDTO("key"))));

    var responses = responseFutures.get();
    StorageResponse.Success writeResponse = dispatcherHelper.getResponseAsSuccess(responses.get(0));
    StorageResponse.Success deleteResponse = dispatcherHelper.getResponseAsSuccess(
        responses.get(1));
    StorageResponse.Success readResponse = dispatcherHelper.getResponseAsSuccess(responses.get(2));

    assertThat(writeResponse.message()).isEqualTo("OK");
    assertThat(deleteResponse.message()).isEqualTo("OK");
    assertThat(readResponse.message()).isEqualTo("[key] not found");
  }
}

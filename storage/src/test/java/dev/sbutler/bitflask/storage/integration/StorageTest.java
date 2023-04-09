package dev.sbutler.bitflask.storage.integration;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({StorageExtension.class, ListeningExecutorServiceExtension.class})
public class StorageTest {

  private final StorageCommandDispatcherHelper dispatcherHelper;

  public StorageTest(StorageCommandDispatcher commandDispatcher,
      ListeningExecutorService listeningExecutorService) {
    this.dispatcherHelper = new StorageCommandDispatcherHelper(commandDispatcher,
        listeningExecutorService);
  }

  @Test
  public void test() throws Exception {
    ImmutableList<ListenableFuture<StorageResponse>> responses =
        dispatcherHelper.submitStorageCommandsSequentially(ImmutableList.of(
            new WriteDTO("key", "value"),
            new ReadDTO("key")));

    Thread.sleep(100L);

    StorageResponse writeResponse = responses.get(0).get();
    assertThat(writeResponse).isInstanceOf(StorageResponse.Success.class);
    StorageResponse readResponse = responses.get(1).get();
    assertThat(readResponse).isInstanceOf(StorageResponse.Success.class);
  }
}

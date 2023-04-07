package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(StorageExtension.class)
public class StorageTest {

  private final StorageCommandDispatcher commandDispatcher;

  public StorageTest(StorageCommandDispatcher commandDispatcher) {
    this.commandDispatcher = commandDispatcher;
  }

  @Test
  public void test() throws Exception {
    ImmutableList<ListenableFuture<StorageResponse>> commands = ImmutableList.of(
        commandDispatcher.put(new WriteDTO("key", "value")),
        commandDispatcher.put(new ReadDTO("key")));

    Thread.sleep(100L);

    assertThat(commands.get(0).isDone()).isTrue();
    StorageResponse writeResponse = commands.get(0).get();
    assertThat(writeResponse).isInstanceOf(StorageResponse.Success.class);
    assertThat(commands.get(1).isDone()).isTrue();
    StorageResponse readResponse = commands.get(1).get();
    assertThat(readResponse).isInstanceOf(StorageResponse.Success.class);
  }
}

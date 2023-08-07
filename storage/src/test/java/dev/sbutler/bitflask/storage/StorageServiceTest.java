package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.commands.ClientCommand;
import dev.sbutler.bitflask.storage.commands.ClientCommandMapper;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import org.junit.jupiter.api.Test;

class StorageServiceTest {

  private final LSMTree lsmTree = mock(LSMTree.class);
  private final ClientCommandMapper clientCommandMapper = mock(ClientCommandMapper.class);
  private final StorageLoader storageLoader = mock(StorageLoader.class);

  private final StorageService storageService =
      new StorageService(lsmTree, clientCommandMapper, storageLoader);

  @Test
  public void startUp() {
    storageService.startUp();

    verify(storageLoader, times(1)).load();
  }

  @Test
  public void shutdown() {
    storageService.shutDown();
    verify(lsmTree, times(1)).close();
  }

  @Test
  public void processCommand() {
    ReadDTO dto = new ReadDTO("key");
    ClientCommand clientCommand = mock(ClientCommand.class);
    StorageSubmitResults expectedResponse =
        new StorageSubmitResults.Success(immediateFuture("value"));
    when(clientCommand.execute()).thenReturn(expectedResponse);
    when(clientCommandMapper.mapToCommand(any())).thenReturn(clientCommand);

    StorageSubmitResults response = storageService.processCommand(dto);

    assertThat(response).isEqualTo(expectedResponse);
  }
}

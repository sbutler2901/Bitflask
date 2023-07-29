package dev.sbutler.bitflask.storage;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.commands.CommandMapper;
import dev.sbutler.bitflask.storage.commands.StorageCommand;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import org.junit.jupiter.api.Test;

class StorageServiceTest {

  private final LSMTree lsmTree = mock(LSMTree.class);
  private final CommandMapper commandMapper = mock(CommandMapper.class);
  private final StorageLoader storageLoader = mock(StorageLoader.class);

  private final StorageService storageService =
      new StorageService(lsmTree, commandMapper, storageLoader);

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
    StorageCommand command = mock(StorageCommand.class);
    StorageResponse expectedResponse = new Success("value");
    when(command.execute()).thenReturn(expectedResponse);
    when(commandMapper.mapToCommand(any())).thenReturn(command);

    StorageResponse response = storageService.processCommand(dto);

    assertThat(response).isEqualTo(expectedResponse);
  }
}

package dev.sbutler.bitflask.storage.commands;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.commands.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Failed;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import org.junit.jupiter.api.Test;

public class DeleteCommandTest {

  private final DeleteDTO DTO = new DeleteDTO("key");

  private final LSMTree lsmTree = mock(LSMTree.class);

  private final DeleteCommand command = new DeleteCommand(lsmTree, DTO);

  @Test
  void deleteSucceeds_returnsOk() {
    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Success.class);
    assertThat(((Success) response).message()).isEqualTo("OK");
  }

  @Test
  void deleteThrowsStorageException_returnsFailed() {
    doThrow(StorageException.class).when(lsmTree).delete(anyString());

    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Failed.class);
    assertThat(((Failed) response).message())
        .isEqualTo(String.format("Failed to delete [%s]", DTO.key()));
  }
}

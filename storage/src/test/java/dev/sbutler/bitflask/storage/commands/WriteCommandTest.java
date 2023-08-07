package dev.sbutler.bitflask.storage.commands;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Failed;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WriteCommandTest {

  private final WriteDTO DTO = new WriteDTO("key", "value");

  private final LSMTree lsmTree = mock(LSMTree.class);

  private final WriteCommand command = new WriteCommand(lsmTree, DTO);

  @Test
  void writeSucceeds_returnsOk() {
    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Success.class);
    assertThat(((Success) response).message()).isEqualTo("OK");
  }

  @Test
  void writeThrowsStorageException_returnsFailed() {
    doThrow(StorageException.class).when(lsmTree).write(anyString(), anyString());

    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Failed.class);
    assertThat(((Failed) response).message())
        .isEqualTo(String.format("Failed to write [%s]:[%s]", DTO.key(), DTO.value()));
  }
}

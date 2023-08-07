package dev.sbutler.bitflask.storage.commands;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Failed;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults.Success;
import dev.sbutler.bitflask.storage.exceptions.StorageException;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReadCommandTest {

  private final ReadDTO DTO = new ReadDTO("key");

  private final LSMTree lsmTree = mock(LSMTree.class);

  private final ReadCommand command = new ReadCommand(lsmTree, DTO);

  @Test
  public void valueFound() {
    when(lsmTree.read(anyString())).thenReturn(Optional.of("value"));

    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Success.class);
    assertThat(((Success) response).message()).isEqualTo("value");
  }

  @Test
  public void valueNotFound() {
    when(lsmTree.read(anyString())).thenReturn(Optional.empty());

    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Success.class);
    assertThat(((Success) response).message())
        .isEqualTo(String.format("[%s] not found", DTO.key()));
  }

  @Test
  public void readThrowsStorageException_returnsFailed() {
    when(lsmTree.read(anyString())).thenThrow(StorageException.class);

    StorageCommandResults response = command.execute();

    assertThat(response).isInstanceOf(Failed.class);
    assertThat(((Failed) response).message())
        .isEqualTo(String.format("Failed to read [%s]", DTO.key()));
  }
}

package dev.sbutler.bitflask.storage.commands;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.lsm.LSMTree;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link StorageCommandFactory}. */
public class StorageCommandFactoryTest {

  private final LSMTree lsmTree = mock(LSMTree.class);

  private final StorageCommandFactory storageCommandFactory = new StorageCommandFactory(lsmTree);

  @Test
  public void create_provided_readDTO_returnsReadCommand() {
    StorageCommandDto.ReadDto dto = new StorageCommandDto.ReadDto("key");

    StorageCommand command = storageCommandFactory.create(dto);

    assertThat(command).isInstanceOf(ReadCommand.class);
  }

  @Test
  public void create_provided_writeDTO_returnsWriteCommand() {
    StorageCommandDto.WriteDto dto = new StorageCommandDto.WriteDto("key", "value");

    StorageCommand command = storageCommandFactory.create(dto);

    assertThat(command).isInstanceOf(WriteCommand.class);
  }

  @Test
  public void create_provided_deleteDTO_returnsDeleteCommand() {
    StorageCommandDto.DeleteDto dto = new StorageCommandDto.DeleteDto("key");

    StorageCommand command = storageCommandFactory.create(dto);

    assertThat(command).isInstanceOf(DeleteCommand.class);
  }
}

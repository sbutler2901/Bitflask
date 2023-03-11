package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.lsm.LSMTree;
import org.junit.jupiter.api.Test;

public class CommandMapperTest {

  private final LSMTree lsmTree = mock(LSMTree.class);
  private final CommandMapper commandMapper = new CommandMapper(lsmTree);

  @Test
  void readDTO() {
    // Arrange
    ReadDTO dto = new ReadDTO("key");
    // Act
    StorageCommand command = commandMapper.mapToCommand(dto);
    // Assert
    assertInstanceOf(ReadCommand.class, command);
  }

  @Test
  void writeDTO() {
    // Arrange
    WriteDTO dto = new WriteDTO("key", "value");
    // Act
    StorageCommand command = commandMapper.mapToCommand(dto);
    // Assert
    assertInstanceOf(WriteCommand.class, command);
  }

  @Test
  void deleteDTO() {
    // Arrange
    DeleteDTO dto = new DeleteDTO("key");
    // Act
    StorageCommand command = commandMapper.mapToCommand(dto);
    // Assert
    assertInstanceOf(DeleteCommand.class, command);
  }
}

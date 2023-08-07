package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.StorageCommandDTO;
import dev.sbutler.bitflask.storage.raft.Raft;
import org.junit.jupiter.api.Test;

/** Unit test for {@link ClientCommandMapper}. */
public class ClientCommandMapperTest {

  private final Raft raft = mock(Raft.class);
  private final StorageCommandFactory storageCommandFactory = mock(StorageCommandFactory.class);

  private final ClientCommandMapper clientCommandMapper =
      new ClientCommandMapper(raft, storageCommandFactory);

  @Test
  void readDTO() {
    // Arrange
    StorageCommandDTO.ReadDTO dto = new StorageCommandDTO.ReadDTO("key");
    // Act
    ClientCommand command = clientCommandMapper.mapToCommand(dto);
    // Assert
    assertInstanceOf(ClientReadCommand.class, command);
  }

  @Test
  void writeDTO() {
    // Arrange
    StorageCommandDTO.WriteDTO dto = new StorageCommandDTO.WriteDTO("key", "value");
    // Act
    ClientCommand command = clientCommandMapper.mapToCommand(dto);
    // Assert
    assertInstanceOf(ClientWriteCommand.class, command);
  }

  @Test
  void deleteDTO() {
    // Arrange
    StorageCommandDTO.DeleteDTO dto = new StorageCommandDTO.DeleteDTO("key");
    // Act
    ClientCommand command = clientCommandMapper.mapToCommand(dto);
    // Assert
    assertInstanceOf(ClientDeleteCommand.class, command);
  }
}

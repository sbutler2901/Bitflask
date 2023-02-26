package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
import dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CommandMapperTest {

  @InjectMocks
  CommandMapper commandMapper;
  @Mock
  ListeningExecutorService executorService;
  @Mock
  SegmentManagerService segmentManagerService;

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

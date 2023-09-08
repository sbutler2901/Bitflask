package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftCommandConversionException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link RaftEntryConverter}. */
public class RaftEntryConverterTest {

  private final RaftPersistentState raftPersistentState = Mockito.mock(RaftPersistentState.class);

  private final RaftEntryConverter raftEntryConverter = new RaftEntryConverter(raftPersistentState);

  @Test
  public void convertStorageCommandDto_write() {
    when(raftPersistentState.getCurrentTerm()).thenReturn(1);
    var command = new StorageCommandDto.WriteDto("key", "value");

    var entry = raftEntryConverter.convert(command);

    assertThat(entry)
        .isEqualTo(
            Entry.newBuilder()
                .setTerm(1)
                .setSetCommand(
                    SetCommand.newBuilder().setKey(command.key()).setValue(command.value()))
                .build());
  }

  @Test
  public void convertStorageCommandDto_delete() {
    when(raftPersistentState.getCurrentTerm()).thenReturn(1);
    var command = new StorageCommandDto.DeleteDto("key");

    var entry = raftEntryConverter.convert(command);

    assertThat(entry)
        .isEqualTo(
            Entry.newBuilder()
                .setTerm(1)
                .setDeleteCommand(DeleteCommand.newBuilder().setKey(command.key()))
                .build());
  }

  @Test
  public void convertStorageCommandDto_unknown_throwsRaftCommandConversionException() {
    var dto = new StorageCommandDto.ReadDto("key");

    RaftCommandConversionException exception =
        assertThrows(RaftCommandConversionException.class, () -> raftEntryConverter.convert(dto));

    assertThat(exception).hasMessageThat().ignoringCase().contains("Unknown StorageCommandDto");
  }

  @Test
  public void convertEntry_set() {
    var entry =
        Entry.newBuilder()
            .setTerm(1)
            .setSetCommand(SetCommand.newBuilder().setKey("key").setValue("value"))
            .build();

    var command = raftEntryConverter.reverse().convert(entry);

    assertThat(command).isNotNull();
    assertThat(command).isInstanceOf(StorageCommandDto.WriteDto.class);
    StorageCommandDto.WriteDto writeDto = (StorageCommandDto.WriteDto) command;
    assertThat(writeDto.key()).isEqualTo(entry.getSetCommand().getKey());
    assertThat(writeDto.value()).isEqualTo(entry.getSetCommand().getValue());
  }

  @Test
  public void convertEntry_delete() {
    var entry =
        Entry.newBuilder()
            .setTerm(1)
            .setDeleteCommand(DeleteCommand.newBuilder().setKey("key").build())
            .build();

    var command = raftEntryConverter.reverse().convert(entry);

    assertThat(command).isNotNull();
    assertThat(command).isInstanceOf(StorageCommandDto.DeleteDto.class);
    StorageCommandDto.DeleteDto deleteDto = (StorageCommandDto.DeleteDto) command;
    assertThat(deleteDto.key()).isEqualTo(entry.getDeleteCommand().getKey());
  }

  @Test
  public void convertEntry_unknownCommand_throwsRaftCommandConversionException() {
    var entry = Entry.newBuilder().setEmptyCommand(Entry.EmptyCommand.getDefaultInstance()).build();

    RaftCommandConversionException exception =
        assertThrows(
            RaftCommandConversionException.class,
            () -> raftEntryConverter.reverse().convert(entry));

    assertThat(exception).hasMessageThat().ignoringCase().contains("Unknown Entry command case");
  }
}

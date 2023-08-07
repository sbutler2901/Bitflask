package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.storage.raft.exceptions.RaftCommandConversionException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** Unit tests for {@link RaftCommandConverter}. */
public class RaftCommandConverterTest {

  private final RaftPersistentState raftPersistentState = Mockito.mock(RaftPersistentState.class);

  private final RaftCommandConverter raftCommandConverter =
      new RaftCommandConverter(raftPersistentState);

  @Test
  public void convertRaftCommand_set() {
    when(raftPersistentState.getCurrentTerm()).thenReturn(1);
    var command = new RaftCommand.SetCommand("key", "value");

    var entry = raftCommandConverter.convert(command);

    assertThat(entry)
        .isEqualTo(
            Entry.newBuilder()
                .setTerm(1)
                .setSetCommand(
                    SetCommand.newBuilder().setKey(command.key()).setValue(command.value()))
                .build());
  }

  @Test
  public void convertRaftCommand_delete() {
    when(raftPersistentState.getCurrentTerm()).thenReturn(1);
    var command = new RaftCommand.DeleteCommand("key");

    var entry = raftCommandConverter.convert(command);

    assertThat(entry)
        .isEqualTo(
            Entry.newBuilder()
                .setTerm(1)
                .setDeleteCommand(DeleteCommand.newBuilder().setKey(command.key()))
                .build());
  }

  @Test
  public void convertEntry_set() {
    var entry =
        Entry.newBuilder()
            .setTerm(1)
            .setSetCommand(SetCommand.newBuilder().setKey("key").setValue("value"))
            .build();

    var command = raftCommandConverter.reverse().convert(entry);

    assertThat(command).isNotNull();
    assertThat(command).isInstanceOf(RaftCommand.SetCommand.class);
    RaftCommand.SetCommand setCommand = (RaftCommand.SetCommand) command;
    assertThat(setCommand.key()).isEqualTo(entry.getSetCommand().getKey());
    assertThat(setCommand.value()).isEqualTo(entry.getSetCommand().getValue());
  }

  @Test
  public void convertEntry_delete() {
    var entry =
        Entry.newBuilder()
            .setTerm(1)
            .setDeleteCommand(DeleteCommand.newBuilder().setKey("key").build())
            .build();

    var command = raftCommandConverter.reverse().convert(entry);

    assertThat(command).isNotNull();
    assertThat(command).isInstanceOf(RaftCommand.DeleteCommand.class);
    RaftCommand.DeleteCommand setCommand = (RaftCommand.DeleteCommand) command;
    assertThat(setCommand.key()).isEqualTo(entry.getDeleteCommand().getKey());
  }

  @Test
  public void convertEntry_unknownCommand_throwsRaftCommandConversionException() {
    var entry = Entry.newBuilder().setEmptyCommand(Entry.EmptyCommand.getDefaultInstance()).build();

    RaftCommandConversionException exception =
        assertThrows(
            RaftCommandConversionException.class,
            () -> raftCommandConverter.reverse().convert(entry));

    assertThat(exception).hasMessageThat().ignoringCase().contains("Unknown Entry command case");
  }
}

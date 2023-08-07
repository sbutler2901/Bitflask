package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Converter;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftCommandConversionException;
import jakarta.inject.Inject;

/**
 * Handles converting between {@link RaftCommand} and {@link
 * dev.sbutler.bitflask.storage.raft.Entry} instance.
 */
final class RaftCommandConverter extends Converter<RaftCommand, Entry> {

  private final RaftPersistentState raftPersistentState;

  @Inject
  RaftCommandConverter(RaftPersistentState raftPersistentState) {
    this.raftPersistentState = raftPersistentState;
  }

  @Override
  protected Entry doForward(RaftCommand raftCommand) {
    return switch (raftCommand) {
      case RaftCommand.SetCommand cmd -> Entry.newBuilder()
          .setTerm(raftPersistentState.getCurrentTerm())
          .setSetCommand(SetCommand.newBuilder().setKey(cmd.key()).setValue(cmd.value()))
          .build();
      case RaftCommand.DeleteCommand cmd -> Entry.newBuilder()
          .setTerm(raftPersistentState.getCurrentTerm())
          .setDeleteCommand(DeleteCommand.newBuilder().setKey(cmd.key()).build())
          .build();
    };
  }

  @Override
  protected RaftCommand doBackward(Entry entry) {
    return switch (entry.getCommandCase()) {
      case SET_COMMAND -> new RaftCommand.SetCommand(
          entry.getSetCommand().getKey(), entry.getSetCommand().getValue());
      case DELETE_COMMAND -> new RaftCommand.DeleteCommand(entry.getDeleteCommand().getKey());
      default -> throw new RaftCommandConversionException(
          "Unknown Entry command case: " + entry.getCommandCase());
    };
  }
}

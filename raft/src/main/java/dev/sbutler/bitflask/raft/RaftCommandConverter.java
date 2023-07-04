package dev.sbutler.bitflask.raft;

import com.google.common.base.Converter;
import dev.sbutler.bitflask.raft.exceptions.RaftCommandConversionException;

/**
 * Handles converting between {@link dev.sbutler.bitflask.raft.RaftCommand} and {@link
 * dev.sbutler.bitflask.raft.Entry} instance.
 */
final class RaftCommandConverter extends Converter<RaftCommand, Entry> {

  static final RaftCommandConverter INSTANCE = new RaftCommandConverter();

  @Override
  protected Entry doForward(RaftCommand raftCommand) {
    return switch (raftCommand) {
      case RaftCommand.SetCommand cmd -> Entry.newBuilder()
          .setSetCommand(SetCommand.newBuilder().setKey(cmd.key()).setValue(cmd.value()))
          .build();
      case RaftCommand.DeleteCommand cmd -> Entry.newBuilder()
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

  private RaftCommandConverter() {}
}

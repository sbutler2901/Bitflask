package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Converter;
import dev.sbutler.bitflask.storage.commands.StorageCommandDto;
import dev.sbutler.bitflask.storage.raft.exceptions.RaftCommandConversionException;
import jakarta.inject.Inject;

/**
 * Handles converting between {@link StorageCommandDto} and {@link
 * dev.sbutler.bitflask.storage.raft.Entry} instance.
 */
final class RaftEntryConverter extends Converter<StorageCommandDto, Entry> {

  private final RaftPersistentState raftPersistentState;

  @Inject
  RaftEntryConverter(RaftPersistentState raftPersistentState) {
    this.raftPersistentState = raftPersistentState;
  }

  @Override
  protected Entry doForward(StorageCommandDto storageCommandDto) {
    return switch (storageCommandDto) {
      case StorageCommandDto.WriteDto dto -> Entry.newBuilder()
          .setTerm(raftPersistentState.getCurrentTerm())
          .setSetCommand(SetCommand.newBuilder().setKey(dto.key()).setValue(dto.value()))
          .build();
      case StorageCommandDto.DeleteDto dto -> Entry.newBuilder()
          .setTerm(raftPersistentState.getCurrentTerm())
          .setDeleteCommand(DeleteCommand.newBuilder().setKey(dto.key()).build())
          .build();
      default -> throw new RaftCommandConversionException(
          "Unknown StorageCommandDto: " + storageCommandDto);
    };
  }

  @Override
  protected StorageCommandDto doBackward(Entry entry) {
    return switch (entry.getCommandCase()) {
      case SET_COMMAND -> new StorageCommandDto.WriteDto(
          entry.getSetCommand().getKey(), entry.getSetCommand().getValue());
      case DELETE_COMMAND -> new StorageCommandDto.DeleteDto(entry.getDeleteCommand().getKey());
      default -> throw new RaftCommandConversionException(
          "Unknown Entry command case: " + entry.getCommandCase());
    };
  }
}

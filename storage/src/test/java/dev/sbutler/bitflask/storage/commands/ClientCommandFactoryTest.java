package dev.sbutler.bitflask.storage.commands;

import static org.mockito.Mockito.*;

import dev.sbutler.bitflask.storage.raft.Raft;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ClientCommandFactory}. */
public class ClientCommandFactoryTest {

  private final Raft raft = mock(Raft.class);
  private final StorageCommandFactory storageCommandFactory = mock(StorageCommandFactory.class);

  private final ClientCommandFactory clientCommandFactory =
      new ClientCommandFactory(raft, storageCommandFactory);

  @Test
  public void create() {
    StorageCommandDTO.ReadDTO dto = new StorageCommandDTO.ReadDTO("key");

    clientCommandFactory.create(dto);

    verify(storageCommandFactory, times(1)).create(dto);
  }
}

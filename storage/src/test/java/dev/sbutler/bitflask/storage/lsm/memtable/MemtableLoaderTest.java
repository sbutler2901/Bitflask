package dev.sbutler.bitflask.storage.lsm.memtable;

import static org.mockito.Mockito.mock;

import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;

public class MemtableLoaderTest {

  private final StorageConfigurations config = mock(StorageConfigurations.class);

  private final MemtableLoader memtableLoader = new MemtableLoader(config);
}

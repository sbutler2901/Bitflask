package dev.sbutler.bitflask.storage.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConfigurationModuleTest {

  @Test
  void provideStorageDispatcherCapacity() {
    assertEquals(500, ConfigurationModule.getInstance().provideStorageDispatcherCapacity());
  }
}

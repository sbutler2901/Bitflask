package dev.sbutler.bitflask.server.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ServerModuleTest {

  private final ServerModule serverModule = ServerModule.getInstance();

  @Test
  void provideServerPort() {
    assertEquals(9090, serverModule.provideServerPort());
  }

}

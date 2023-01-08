package dev.sbutler.bitflask.client;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import org.junit.jupiter.api.Test;

public class ClientModuleTest {

  @Test
  void configure() {
    // Arrange
    ClientModule clientModule = new ClientModule.Builder()
        .build();
    Injector injector = Guice.createInjector(clientModule);
    // Act / Assert
    injector.getBinding(ClientConfigurations.class);
  }
}

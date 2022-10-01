package dev.sbutler.bitflask.server.network_service.client_handling_service;

import static org.junit.jupiter.api.Assertions.fail;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import org.junit.jupiter.api.Test;

public class ClientHandlingServiceParentModuleTest {

  @Test
  void configure() {
    Injector injector = Guice.createInjector(new ClientHandlingServiceParentModule());
    try {
      injector.getBinding(CommandProcessingService.class);
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }
}

package dev.sbutler.bitflask.server.network_service.client_handling_service;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.server.configuration.concurrency.ConcurrencyModule;
import dev.sbutler.bitflask.storage.StorageServiceModule;

/**
 * This Guice module provides the common resources shared across all instances of
 * ClientHandlingServices.
 *
 * <p>This is designed to be used to create a parent Guice injector from which child injectors will
 * be created.
 */
public class ClientHandlingServiceParentModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    install(ConcurrencyModule.getInstance());
    install(StorageServiceModule.getInstance());
  }
}

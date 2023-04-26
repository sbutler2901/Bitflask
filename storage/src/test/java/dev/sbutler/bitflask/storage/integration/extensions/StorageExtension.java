package dev.sbutler.bitflask.storage.integration.extensions;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
import dev.sbutler.bitflask.storage.StorageService;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurations;
import dev.sbutler.bitflask.storage.configuration.StorageConfigurationsConstants;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDispatcher;
import java.time.Duration;
import java.util.ResourceBundle;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Sets up a functioning storage instance to interact with in an integration test class.
 *
 * <p>A {@link StorageCommandDispatcher} instance will injectable for a test class that is extended
 * by this extension.
 *
 * <p>This extension can be programmatically added to a test class with the ability to specify
 * custom {@link StorageConfigurations} for the service instance created.
 *
 * <p>The storage instance will exist for the life of the test class that is extended by this
 * extension. Will the lifecycle of all resources managed by this extension.
 */
@SuppressWarnings("UnstableApiUsage")
public class StorageExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Namespace NAMESPACE = Namespace
      .create(StorageExtension.class);

  private static final String TEST_RESOURCE_NAME = "test";

  private final StorageConfigurations configurations;
  private ExtensionStoreHelper storeHelper;

  /**
   * Creates a new instance using the default testing properties file.
   */
  public StorageExtension() {
    this.configurations = initializeConfiguration(new String[0]);
  }

  public StorageExtension(StorageConfigurations configurations) {
    this.configurations = configurations;
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    printConfigInfo(configurations);

    storeHelper = new ExtensionStoreHelper(extensionContext.getStore(NAMESPACE));

    var injector = Guice.createInjector(ImmutableSet.of(
        new VirtualThreadConcurrencyModule(),
        new StorageServiceModule(configurations)));

    var serviceManager = new ServiceManager(ImmutableSet.of(
        injector.getInstance(StorageService.class)));

    serviceManager.startAsync().awaitHealthy(Duration.ofSeconds(5));

    storeHelper.putInStore(Injector.class, injector);
    storeHelper.putInStore(serviceManager);

    logger.atFine().log("Storage initialized");
  }

  @Override
  public void afterAll(ExtensionContext context) {
    try {
      var serviceManager = storeHelper.getFromStore(ServiceManager.class);
      serviceManager.stopAsync().awaitStopped(Duration.ofSeconds(5));
    } catch (TimeoutException timeout) {
      logger.atSevere().withCause(timeout).log("ServiceManager timed out while stopping");
    }

    var listeningExecutorService = storeHelper.getFromStore(Injector.class)
        .getInstance(Injector.class).getInstance(ListeningExecutorService.class);
    MoreExecutors.shutdownAndAwaitTermination(listeningExecutorService, Duration.ofSeconds(5));

    logger.atFine().log("Storage terminated");
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return StorageCommandDispatcher.class.equals(parameterContext.getParameter().getType())
        || StorageConfigurations.class.equals(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    if (StorageConfigurations.class.equals(parameterContext.getParameter().getType())) {
      return configurations;
    }
    return storeHelper.getFromStore(Injector.class)
        .getInstance(parameterContext.getParameter().getType());
  }

  private static StorageConfigurations initializeConfiguration(String[] args) {
    ResourceBundle resourceBundle = ResourceBundle.getBundle(TEST_RESOURCE_NAME);
    ConfigurationsBuilder configurationsBuilder = new ConfigurationsBuilder(args, resourceBundle);

    StorageConfigurations storageConfigurations = new StorageConfigurations();
    configurationsBuilder.buildAcceptingUnknownOptions(storageConfigurations,
        StorageConfigurationsConstants.STORAGE_FLAG_TO_CONFIGURATION_MAP);
    return storageConfigurations;
  }

  private static void printConfigInfo(StorageConfigurations storageConfigurations) {
    logger.atFine().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atFine()
        .log("Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());

    logger.atFine().log(storageConfigurations.toString());
  }

}

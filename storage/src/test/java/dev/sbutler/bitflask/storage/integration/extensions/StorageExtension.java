package dev.sbutler.bitflask.storage.integration.extensions;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import dev.sbutler.bitflask.config.BitflaskConfig;
import dev.sbutler.bitflask.config.ConfigDefaults;
import dev.sbutler.bitflask.config.ConfigModule;
import dev.sbutler.bitflask.config.StorageConfig;
import dev.sbutler.bitflask.storage.StorageService;
import dev.sbutler.bitflask.storage.StorageServiceModule;
import dev.sbutler.bitflask.storage.commands.ClientCommand;
import java.time.Duration;
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
 * <p>A {@link StorageService} instance will injectable for a test class that is extended by this
 * extension.
 *
 * <p>This extension can be programmatically added to a test class with the ability to specify
 * custom {@link StorageConfig} for the service instance created.
 *
 * <p>The storage instance will exist for the life of the test class that is extended by this
 * extension. Will the lifecycle of all resources managed by this extension.
 */
public class StorageExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Namespace NAMESPACE = Namespace.create(StorageExtension.class);

  private final StorageConfig storageConfig;
  private ExtensionStoreHelper storeHelper;

  /**
   * Creates a new instance using the default testing properties file.
   *
   * <p>This constructor is used by the Junit @ExtendWith annotation
   */
  @SuppressWarnings("unused")
  public StorageExtension() {
    this.storageConfig = ConfigDefaults.STORAGE_CONFIG;
  }

  /**
   * Creates a new instance using the provided configurations.
   *
   * <p>This constructor is used when instantiated with a test class.
   */
  @SuppressWarnings("unused")
  public StorageExtension(StorageConfig storageConfig) {
    this.storageConfig = storageConfig;
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    printConfigInfo(storageConfig);

    storeHelper = new ExtensionStoreHelper(extensionContext.getStore(NAMESPACE));

    var storageServiceModule = new StorageServiceModule();
    var injector =
        Guice.createInjector(
            ImmutableSet.of(
                new ConfigModule(
                    BitflaskConfig.newBuilder().setStorageConfig(storageConfig).build()),
                new VirtualThreadConcurrencyModule(),
                storageServiceModule));

    var serviceManager = new ServiceManager(storageServiceModule.getServices(injector));

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

    var listeningExecutorService =
        storeHelper
            .getFromStore(Injector.class)
            .getInstance(Injector.class)
            .getInstance(ListeningExecutorService.class);
    MoreExecutors.shutdownAndAwaitTermination(listeningExecutorService, Duration.ofSeconds(5));

    logger.atFine().log("Storage terminated");
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return ClientCommand.Factory.class.equals(parameterContext.getParameter().getType())
        || StorageConfig.class.equals(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    if (StorageConfig.class.equals(parameterContext.getParameter().getType())) {
      return storageConfig;
    }
    return storeHelper
        .getFromStore(Injector.class)
        .getInstance(parameterContext.getParameter().getType());
  }

  private static void printConfigInfo(StorageConfig storageConfig) {
    logger.atFine().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atFine().log(
        "Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());

    logger.atFine().log(storageConfig.toString());
  }
}

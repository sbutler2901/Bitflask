package dev.sbutler.bitflask.storage;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import dev.sbutler.bitflask.common.configuration.ConfigurationsBuilder;
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

@SuppressWarnings("UnstableApiUsage")
public class StorageExtension implements ParameterResolver, BeforeAllCallback, AfterAllCallback {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Namespace NAMESPACE = Namespace
      .create(StorageExtension.class);

  private static final String TEST_RESOURCE_NAME = "test";

  private final StorageConfigurations configurations;

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
  public void beforeAll(ExtensionContext context) {
    var injector = Guice.createInjector(ImmutableSet.of(
        new VirtualThreadConcurrencyModule(),
        new StorageServiceModule(configurations)));

    var serviceManager = new ServiceManager(ImmutableSet.of(
        injector.getInstance(StorageService.class)));

    putInStore(context, injector);
    putInStore(context, serviceManager);

    printConfigInfo(configurations);
  }

  @Override
  public void afterAll(ExtensionContext context) {
    try {
      var serviceManager = getFromStore(context, ServiceManager.class);
      serviceManager.stopAsync().awaitStopped(Duration.ofSeconds(5));
    } catch (TimeoutException timeout) {
      logger.atSevere().withCause(timeout).log("ServiceManager timed out while stopping");
    }

    var listeningExecutorService = getFromStore(context, Injector.class)
        .getInstance(Injector.class).getInstance(ListeningExecutorService.class);
    shutdownAndAwaitTermination(listeningExecutorService, Duration.ofSeconds(5));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return StorageCommandDispatcher.class.equals(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return getFromStore(extensionContext, extensionContext.getRequiredTestClass());
  }

  private void putInStore(ExtensionContext context, Object object) {
    context.getStore(NAMESPACE).put(object.getClass().getName(), object);
  }

  private <T> T getFromStore(ExtensionContext context, Class<T> clazz) {
    return context.getStore(NAMESPACE).get(clazz.getName(), clazz);
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
    logger.atInfo().log("Using java version [%s]", System.getProperty("java.version"));
    logger.atInfo()
        .log("Runtime processors available [%d]", Runtime.getRuntime().availableProcessors());

    logger.atInfo().log(storageConfigurations.toString());
  }

}

package dev.sbutler.bitflask.storage.integration.extensions;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.concurrency.VirtualThreadConcurrencyModule;
import java.time.Duration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Provides a dedicated {@link com.google.common.util.concurrent.ListeningExecutorService} for usage
 * by tests.
 *
 * <p>The lifecycle of the ListeningExecutorService is managed by this extension.
 */
@SuppressWarnings("UnstableApiUsage")
public class ListeningExecutorServiceExtension implements ParameterResolver, BeforeAllCallback,
    AfterAllCallback {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final Namespace NAMESPACE = Namespace
      .create(ListeningExecutorServiceExtension.class);

  private ExtensionStoreHelper storeHelper;

  @Override
  public void beforeAll(ExtensionContext context) {
    storeHelper = new ExtensionStoreHelper(context.getStore(NAMESPACE));

    Injector injector = Guice.createInjector(new VirtualThreadConcurrencyModule());

    storeHelper.putInStore(ListeningExecutorService.class,
        injector.getInstance(ListeningExecutorService.class));

    logger.atInfo().log("ListeningExecutorService initialized");
  }

  @Override
  public void afterAll(ExtensionContext context) {
    MoreExecutors.shutdownAndAwaitTermination(
        storeHelper.getFromStore(ListeningExecutorService.class),
        Duration.ofMillis(500));

    logger.atInfo().log("ListeningExecutorService shutdown and terminated");
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return ListeningExecutorService.class.equals(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext,
      ExtensionContext extensionContext) throws ParameterResolutionException {
    return storeHelper.getFromStore(ListeningExecutorService.class);
  }
}

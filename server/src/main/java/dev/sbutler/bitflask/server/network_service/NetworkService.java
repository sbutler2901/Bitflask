package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.server.configuration.ServerConfigurations;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles accepting incoming client requests and submitting them for processing.
 */
@Singleton
public final class NetworkService extends AbstractExecutionThreadService implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final ClientHandlingService.Factory clientHandlingServiceFactory;
  private final ServerConfigurations serverConfigurations;

  private ServerSocketChannel serverSocketChannel;
  private final Set<ClientHandlingService> runningClientHandlingServices = ConcurrentHashMap.newKeySet();

  @Inject
  NetworkService(ListeningExecutorService listeningExecutorService,
      ClientHandlingService.Factory clientHandlingServiceFactory,
      ServerConfigurations serverConfigurations) {
    this.listeningExecutorService = listeningExecutorService;
    this.clientHandlingServiceFactory = clientHandlingServiceFactory;
    this.serverConfigurations = serverConfigurations;
  }

  @Override
  protected void startUp() throws IOException {
    this.serverSocketChannel = createServerSocketChannel();
    registerShutdownListener();
  }

  private ServerSocketChannel createServerSocketChannel() throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(serverConfigurations.getPort());
    serverSocketChannel.bind(inetSocketAddress);
    return serverSocketChannel;
  }

  @Override
  protected void run() throws IOException {
    while (isRunning() && serverSocketChannel.isOpen()
        && !Thread.currentThread().isInterrupted()) {
      SocketChannel socketChannel = acceptNextClientConnection();

      ClientHandlingService clientHandlingService =
          clientHandlingServiceFactory.create(socketChannel);
      startClientHandlingService(clientHandlingService);
    }
  }

  private SocketChannel acceptNextClientConnection() throws IOException {
    SocketChannel socketChannel = serverSocketChannel.accept();
    logger.atInfo()
        .log("Received incoming client connection from [%s]", socketChannel.getRemoteAddress());
    return socketChannel;
  }

  private void startClientHandlingService(ClientHandlingService clientHandlingService) {
    runningClientHandlingServices.add(clientHandlingService);

    // Clean up after service has reached a terminal state
    clientHandlingService.startAsync().addListener(new Listener() {
      @SuppressWarnings("NullableProblems")
      @Override
      public void terminated(State from) {
        runningClientHandlingServices.remove(clientHandlingService);
      }

      @SuppressWarnings("NullableProblems")
      @Override
      public void failed(State from, Throwable failure) {
        runningClientHandlingServices.remove(clientHandlingService);
      }
    }, listeningExecutorService);
  }

  /**
   * Handles graceful shutdown when this service has been stopped
   */
  private void registerShutdownListener() {
    this.addListener(new Listener() {
      @SuppressWarnings("NullableProblems")
      @Override
      public void stopping(State from) {
        super.stopping(from);
        close();
        stopAllClientHandlingServices();
      }
    }, listeningExecutorService);
  }

  @Override
  public void close() {
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      logger.atWarning().withCause(e)
          .log("Error closing the ServerSocketChannel");
    }
  }

  private void stopAllClientHandlingServices() {
    runningClientHandlingServices.forEach(ClientHandlingService::stopAsync);
    runningClientHandlingServices.forEach(ClientHandlingService::awaitTerminated);
  }
}

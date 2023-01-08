package dev.sbutler.bitflask.server.network_service;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.server.configuration.ServerConfigurations;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientHandlingService;
import dev.sbutler.bitflask.server.network_service.client_handling_service.ClientMessageProcessor;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles accepting incoming client requests and submitting them for processing.
 */
@Singleton
public final class NetworkService extends AbstractExecutionThreadService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService listeningExecutorService;
  private final ClientMessageProcessor.Factory clientMessageProcessorFactory;
  private final ServerConfigurations serverConfigurations;

  private ServerSocketChannel serverSocketChannel;
  private final Set<ClientHandlingService> runningClientHandlingServices = new HashSet<>();
  private volatile boolean shouldContinueRunning = true;

  @Inject
  NetworkService(ListeningExecutorService listeningExecutorService,
      ClientMessageProcessor.Factory clientMessageProcessorFactory,
      ServerConfigurations serverConfigurations) {
    this.listeningExecutorService = listeningExecutorService;
    this.clientMessageProcessorFactory = clientMessageProcessorFactory;
    this.serverConfigurations = serverConfigurations;
  }

  @Override
  protected void startUp() throws IOException {
    logger.atFine().log("NetworkService: startUp start");
    this.serverSocketChannel = createServerSocketChannel();
    logger.atFine().log("NetworkService: startUp end");
  }

  private ServerSocketChannel createServerSocketChannel() throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    InetSocketAddress inetSocketAddress = new InetSocketAddress(serverConfigurations.getPort());
    serverSocketChannel.bind(inetSocketAddress);
    return serverSocketChannel;
  }

  @Override
  protected void run() throws IOException {
    logger.atFine().log("NetworkService: starting run");
    while (shouldContinueRunning && serverSocketChannel.isOpen()
        && !Thread.currentThread().isInterrupted()) {
      acceptAndExecuteNextClientConnection();
    }
    logger.atFine().log("NetworkService: stopping run");
  }

  private void acceptAndExecuteNextClientConnection() throws IOException {
    try {
      SocketChannel socketChannel = serverSocketChannel.accept();
      logger.atInfo()
          .log("Received incoming client connection from [%s]", socketChannel.getRemoteAddress());

      ClientHandlingService clientHandlingService = createClientHandlingService(socketChannel);
      submitClientHandlingService(clientHandlingService);
    } catch (ClosedChannelException e) {
      logger.atInfo().log("ServerSocketChannel closed");
    }
  }

  private ClientHandlingService createClientHandlingService(SocketChannel socketChannel)
      throws IOException {
    RespService respService = RespService.create(socketChannel);
    ClientMessageProcessor clientMessageProcessor =
        clientMessageProcessorFactory.create(respService);
    return ClientHandlingService.create(respService, clientMessageProcessor);
  }

  private void submitClientHandlingService(ClientHandlingService clientHandlingService) {
    runningClientHandlingServices.add(clientHandlingService);
    Futures.submit(clientHandlingService, listeningExecutorService)
        .addListener(() -> runningClientHandlingServices.remove(clientHandlingService),
            listeningExecutorService);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    logger.atInfo().log("NetworkService: shutdown triggered");
    shouldContinueRunning = false;
    try {
      serverSocketChannel.close();
    } catch (IOException e) {
      System.err.println("Error closing NetworkService's ServerSocketChannel" + e);
    }
    runningClientHandlingServices.forEach(ClientHandlingService::close);
  }
}

package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.config.ServerConfig;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** The Raft gRPC server. */
@Singleton
public final class RaftService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final ListeningExecutorService executorService;
  private final RaftConfiguration raftConfiguration;
  private final RaftRpcService raftRpcService;
  private final RaftLoader raftLoader;

  private Server rpcServer;

  @Inject
  RaftService(
      ListeningExecutorService executorService,
      RaftConfiguration raftConfiguration,
      RaftRpcService raftRpcService,
      RaftLoader raftLoader) {
    this.executorService = executorService;
    this.raftConfiguration = raftConfiguration;
    this.raftRpcService = raftRpcService;
    this.raftLoader = raftLoader;
  }

  @Override
  protected void startUp() throws IOException {
    raftLoader.load();
    startRpcServer();
  }

  private void startRpcServer() throws IOException {
    ServerConfig.ServerInfo thisRaftServerInfo = raftConfiguration.getThisServerInfo();
    rpcServer =
        Grpc.newServerBuilderForPort(
                thisRaftServerInfo.getRaftPort(), InsecureServerCredentials.create())
            .executor(executorService)
            .addService(raftRpcService)
            .build();
    rpcServer.start();
    logger.atInfo().log(
        "RaftService [%s] started on [%s:%s]",
        thisRaftServerInfo.getServerId(),
        thisRaftServerInfo.getHost(),
        thisRaftServerInfo.getRaftPort());
  }

  @Override
  protected void shutDown() throws Exception {
    rpcServer.shutdown().awaitTermination(30, TimeUnit.SECONDS);
  }
}

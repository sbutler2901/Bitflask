package dev.sbutler.bitflask.storage.raft;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.config.ServerConfig;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;

/** The Raft gRPC server. */
@Singleton
public final class RaftService extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftConfiguration raftConfiguration;
  private final RaftRpcService raftRpcService;

  private Server server;

  @Inject
  RaftService(RaftConfiguration raftConfiguration, RaftRpcService raftRpcService) {
    this.raftConfiguration = raftConfiguration;
    this.raftRpcService = raftRpcService;
  }

  @Override
  protected void startUp() throws Exception {
    ServerConfig.ServerInfo thisRaftServerInfo = raftConfiguration.getThisServerInfo();
    server =
        Grpc.newServerBuilderForPort(
                thisRaftServerInfo.getRaftPort(), InsecureServerCredentials.create())
            .addService(raftRpcService)
            .build();
    server.start();
    logger.atInfo().log(
        "RaftService [%s] started on [%s:%s]",
        thisRaftServerInfo.getServerId(),
        thisRaftServerInfo.getHost(),
        thisRaftServerInfo.getRaftPort());
  }

  @Override
  protected void shutDown() throws Exception {
    server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
  }
}

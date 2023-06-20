package dev.sbutler.bitflask.raft;

import com.google.common.flogger.FluentLogger;
import com.google.common.util.concurrent.AbstractIdleService;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;

/** The Raft gRPC server. */
@Singleton
public final class RaftServer extends AbstractIdleService {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final RaftServerInfo raftServerInfo;
  private final RaftService raftService;

  private Server server;

  @Inject
  RaftServer(RaftServerInfo raftServerInfo, RaftService raftService) {
    this.raftServerInfo = raftServerInfo;
    this.raftService = raftService;
  }

  @Override
  protected void startUp() throws Exception {
    server =
        Grpc.newServerBuilderForPort(raftServerInfo.port(), InsecureServerCredentials.create())
            .addService(raftService)
            .build();
    server.start();
    logger.atInfo().log(
        "RaftServer [%s] started on [%s:%s]",
        raftServerInfo.id(), raftServerInfo.host(), raftServerInfo.port());
  }

  @Override
  protected void shutDown() throws Exception {
    server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
  }
}

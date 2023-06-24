package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ListeningExecutorService;
import dev.sbutler.bitflask.raft.RaftGrpc.RaftFutureStub;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.concurrent.TimeUnit;

/**
 * Manager of the rpc channels used by this Raft server for communicating with other servers in the
 * raft cluster.
 */
@Singleton
final class RaftClusterRpcChannelManager extends AbstractIdleService {

  private final RaftClusterConfiguration raftClusterConfiguration;
  private final ListeningExecutorService executorService;

  private ImmutableMap<RaftServerId, ManagedChannel> otherServerChannels;
  private ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  @Inject
  RaftClusterRpcChannelManager(
      RaftClusterConfiguration raftClusterConfiguration, ListeningExecutorService executorService) {
    this.raftClusterConfiguration = raftClusterConfiguration;
    this.executorService = executorService;
  }

  @Override
  protected void startUp() {
    ImmutableMap.Builder<RaftServerId, ManagedChannel> managedChannelsBuilder =
        ImmutableMap.builder();
    for (var serverEntry : raftClusterConfiguration.getOtherServersInCluster().entrySet()) {
      String target =
          String.format("%s:%s", serverEntry.getValue().host(), serverEntry.getValue().port());
      ManagedChannel managedChannel =
          Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
      managedChannelsBuilder.put(serverEntry.getKey(), managedChannel);
    }
    otherServerChannels = managedChannelsBuilder.build();

    ImmutableMap.Builder<RaftServerId, RaftFutureStub> stubsBuilder = ImmutableMap.builder();
    for (var channelEntry : otherServerChannels.entrySet()) {
      RaftFutureStub stub = RaftGrpc.newFutureStub(channelEntry.getValue());
      stubsBuilder.put(channelEntry.getKey(), stub);
    }
    otherServerStubs = stubsBuilder.build();
  }

  @Override
  protected void shutDown() throws Exception {
    for (var managedChannel : otherServerChannels.values()) {
      managedChannel.shutdownNow();
    }
    for (var managedChannel : otherServerChannels.values()) {
      managedChannel.awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  RaftClusterCandidateRpcClient createRaftClusterCandidateRpcClient() {
    return new RaftClusterCandidateRpcClient(executorService, otherServerStubs);
  }
}

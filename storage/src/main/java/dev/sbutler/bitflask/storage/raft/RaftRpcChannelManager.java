package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AbstractIdleService;
import dev.sbutler.bitflask.storage.raft.RaftGrpc.RaftFutureStub;
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
final class RaftRpcChannelManager extends AbstractIdleService {

  private final RaftConfiguration raftConfiguration;

  private ImmutableMap<RaftServerId, ManagedChannel> otherServerChannels;
  private ImmutableMap<RaftServerId, RaftFutureStub> otherServerStubs;

  @Inject
  RaftRpcChannelManager(RaftConfiguration raftConfiguration) {
    this.raftConfiguration = raftConfiguration;
  }

  @Override
  protected void startUp() {
    ImmutableMap.Builder<RaftServerId, ManagedChannel> managedChannelsBuilder =
        ImmutableMap.builder();
    for (var serverEntry : raftConfiguration.getOtherServersInCluster().entrySet()) {
      String target =
          String.format(
              "%s:%s", serverEntry.getValue().getHost(), serverEntry.getValue().getRaftPort());
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

  RaftFutureStub getStubForServer(RaftServerId raftServerId) {
    Preconditions.checkArgument(
        otherServerStubs.containsKey(raftServerId),
        String.format("Server [%s] not found in server stubs.", raftServerId.id()));
    return otherServerStubs.get(raftServerId);
  }

  ImmutableMap<RaftServerId, RaftFutureStub> getOtherServerStubs() {
    return otherServerStubs;
  }
}

package dev.sbutler.bitflask.server.configuration;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import dev.sbutler.bitflask.common.guice.RootModule;
import dev.sbutler.bitflask.server.network_service.NetworkService;
import java.nio.channels.ServerSocketChannel;

public class ServerModule extends RootModule {

  private final ServerSocketChannel serverSocketChannel;

  public ServerModule(ServerSocketChannel serverSocketChannel) {
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of(
        injector.getInstance(NetworkService.Factory.class).create(serverSocketChannel));
  }
}

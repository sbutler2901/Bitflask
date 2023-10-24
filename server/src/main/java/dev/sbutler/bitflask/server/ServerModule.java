package dev.sbutler.bitflask.server;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import dev.sbutler.bitflask.common.guice.RootModule;
import java.nio.channels.ServerSocketChannel;

public class ServerModule extends RootModule {

  private final ServerSocketChannel serverSocketChannel;

  public ServerModule(ServerSocketChannel serverSocketChannel) {
    this.serverSocketChannel = serverSocketChannel;
  }

  @Override
  protected void configure() {
    install(new FactoryModuleBuilder().build(RespNetworkService.Factory.class));
    install(new FactoryModuleBuilder().build(RespClientMessageProcessor.Factory.class));
  }

  @Override
  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of(
        injector.getInstance(RespNetworkService.Factory.class).create(serverSocketChannel));
  }
}

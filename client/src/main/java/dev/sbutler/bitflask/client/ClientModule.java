package dev.sbutler.bitflask.client;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import dev.sbutler.bitflask.client.client_processing.ClientProcessingModule;
import dev.sbutler.bitflask.client.network.NetworkModule;

public class ClientModule extends AbstractModule {

  private final ImmutableList<Module> runtimeModules;

  private ClientModule(ImmutableList<Module> runtimeModules) {
    this.runtimeModules = runtimeModules;
  }

  @Override
  protected void configure() {
    runtimeModules.forEach(this::install);
    install(new NetworkModule());
    install(new ClientProcessingModule());
  }

  public static class Builder {

    ImmutableList.Builder<Module> runtimeModules
        = new ImmutableList.Builder<>();

    public Builder addRuntimeModule(Module module) {
      runtimeModules.add(module);
      return this;
    }

    public ClientModule build() {
      return new ClientModule(runtimeModules.build());
    }
  }
}

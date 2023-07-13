package dev.sbutler.bitflask.common.guice;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;

/** A root module for discrete units of the Bitflask server. */
public abstract class RootModule extends AbstractModule {

  /** Returns any {@link Service}s that should be managed by the running Bitflask server. */
  public ImmutableSet<Service> getServices(Injector injector) {
    return ImmutableSet.of();
  }
}

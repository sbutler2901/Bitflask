package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import dev.sbutler.bitflask.client.repl.Repl;

public class ClientModule extends AbstractModule {

  @Provides
  ClientProcessor provideClientProcessor(Repl repl) {
    return repl;
  }
}

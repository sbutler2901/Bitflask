package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import dev.sbutler.bitflask.client.repl.Repl;

public class ClientModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(ClientProcessor.class).to(Repl.class);
  }
}

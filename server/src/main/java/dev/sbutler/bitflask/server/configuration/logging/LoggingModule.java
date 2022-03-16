package dev.sbutler.bitflask.server.configuration.logging;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;

public class LoggingModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    bindListener(Matchers.any(), new Slf4jTypeListener());
  }
}

package dev.sbutler.bitflask.storage.configuration.logging;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import java.lang.reflect.Field;
import org.slf4j.Logger;

/**
 * Called by Guice when bound as a listener in a module. This listener is called when a new type is
 * eligible to be injected via Guice. If the type contains an SLF4J Logger field with the
 * InjectLogger annotation it will be registered to have an SLF4j logger instance injected. <a
 * href="https://github.com/google/guice/wiki/CustomInjections#example-injecting-a-log4j-logger">...</a>
 */
class Slf4jTypeListener implements TypeListener {

  public <I> void hear(TypeLiteral<I> guiceEligibleTypeLiteral,
      TypeEncounter<I> guiceTypeEncounterContext) {
    for (Field field : guiceEligibleTypeLiteral.getRawType().getDeclaredFields()) {
      if (field.getType() == Logger.class && field.isAnnotationPresent(InjectStorageLogger.class)) {
        guiceTypeEncounterContext.register(new Slf4jMembersInjector<>(field));
      }
    }
  }
}

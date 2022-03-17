package dev.sbutler.bitflask.server.configuration.logging;

import com.google.inject.MembersInjector;
import java.lang.reflect.Field;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used by Guice and the Slf4jTypeListener to inject an SLF4J logger into a class's Logger field.
 * https://github.com/google/guice/wiki/CustomInjections#example-injecting-a-log4j-logger
 */
class Slf4jMembersInjector<T> implements MembersInjector<T> {

  private final Field loggerField;
  private final Logger logger;

  Slf4jMembersInjector(Field loggerField) {
    this.loggerField = loggerField;
    logger = LoggerFactory.getLogger(loggerField.getDeclaringClass());
    loggerField.setAccessible(true);
  }

  public void injectMembers(T classWithLoggerField) {
    try {
      loggerField.set(classWithLoggerField, logger);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
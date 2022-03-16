package dev.sbutler.bitflask.server.configuration.logging;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import java.lang.reflect.Field;
import org.slf4j.Logger;

public class Slf4jTypeListener implements TypeListener {

  public <I> void hear(TypeLiteral<I> aTypeLiteral, TypeEncounter<I> aTypeEncounter) {

    for (Field field : aTypeLiteral.getRawType().getDeclaredFields()) {
      if (field.getType() == Logger.class
          && field.isAnnotationPresent(InjectLogger.class)) {
        aTypeEncounter.register(new Slf4jMembersInjector<>(field));
      }
    }
  }
}

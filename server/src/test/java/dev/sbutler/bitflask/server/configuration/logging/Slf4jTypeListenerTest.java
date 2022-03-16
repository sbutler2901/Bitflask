package dev.sbutler.bitflask.server.configuration.logging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeEncounter;
import org.junit.jupiter.api.Test;

public class Slf4jTypeListenerTest {

  @Test
  @SuppressWarnings("unchecked")
  void hear() {
    Slf4jTypeListener slf4jTypeListener = new Slf4jTypeListener();
    TypeLiteral<MockLoggerField> mockLoggerFieldTypeLiteral = mock(TypeLiteral.class);
    TypeEncounter<MockLoggerField> typeEncounter = mock(TypeEncounter.class);
    doReturn(MockLoggerField.class).when(mockLoggerFieldTypeLiteral).getRawType();

    slf4jTypeListener.hear(mockLoggerFieldTypeLiteral, typeEncounter);

    verify(typeEncounter, times(1)).register(any(Slf4jMembersInjector.class));
  }

}

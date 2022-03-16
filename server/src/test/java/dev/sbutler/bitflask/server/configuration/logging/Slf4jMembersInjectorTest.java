package dev.sbutler.bitflask.server.configuration.logging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

public class Slf4jMembersInjectorTest {

  Slf4jMembersInjector<MockLoggerField> slf4jMembersInjector;
  Field field;

  @BeforeEach
  void beforeEach() {
    field = mock(Field.class);
    doReturn(MockLoggerField.class).when(field).getDeclaringClass();
    slf4jMembersInjector = new Slf4jMembersInjector<>(field);
  }

  @Test
  void injectMembers() throws IllegalAccessException {
    MockLoggerField mockLoggerField = new MockLoggerField();
    slf4jMembersInjector.injectMembers(mockLoggerField);
    verify(field, times(1)).set(any(MockLoggerField.class), any(Logger.class));
  }

  @Test
  void injectMembers_IllegalAccessException() throws IllegalAccessException {
    MockLoggerField mockLoggerField = new MockLoggerField();
    doThrow(new IllegalAccessException("test")).when(field)
        .set(any(MockLoggerField.class), any());
    assertThrows(RuntimeException.class, () -> slf4jMembersInjector.injectMembers(mockLoggerField));
  }

}

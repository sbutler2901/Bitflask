package dev.sbutler.bitflask.client.client_processing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReaderProviderTest {

  private ReaderProvider readerProvider;
  private ClientConfigurations configs;

  @BeforeEach
  void beforeEach() {
    configs = mock(ClientConfigurations.class);
    readerProvider = new ReaderProvider(configs);
  }

  @Test
  void usePrompt() {
    // Arrange
    when(configs.getUsePrompt()).thenReturn(true);
    // Act
    Reader reader = readerProvider.get();
    // Assert
    assertThat(reader).isInstanceOf(InputStreamReader.class);
  }

  @Test
  void inline() {
    // Arrange
    when(configs.getUsePrompt()).thenReturn(false);
    // Act
    Reader reader = readerProvider.get();
    // Assert
    assertThat(reader).isInstanceOf(StringReader.class);
  }
}

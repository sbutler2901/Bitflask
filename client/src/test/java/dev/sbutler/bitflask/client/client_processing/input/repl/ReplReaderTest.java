package dev.sbutler.bitflask.client.client_processing.input.repl;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class ReplReaderTest {

  @Test
  void spaceSeparatedStrings() throws Exception {
    // Arrange
    String value = "set test value";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    assertThat(elements).containsExactly("set", "test", "value").inOrder();
  }

}

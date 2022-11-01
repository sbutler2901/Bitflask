package dev.sbutler.bitflask.client.client_processing.repl;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplString;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class ReplParserTest {

  @Test
  void readNextLine() throws Exception {
    // Arrange
    String value = "set test value";
    ReplReader replReader = new ReplReader(new StringReader(value));
    // Act
    ImmutableList<ReplElement> elements = ReplParser.readNextLine(replReader).orElseThrow();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void cleanupForNextLine() throws Exception {
    // Arrange
    ReplReader replReader = new ReplReader(new StringReader("test\nvalue"));
    // Act
    ReplParser.cleanupForNextLine(replReader);
    ImmutableList<ReplElement> elements = ReplParser.readNextLine(replReader).orElseThrow();
    // Assert
    assertThat(elements).hasSize(1);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("value"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }
}

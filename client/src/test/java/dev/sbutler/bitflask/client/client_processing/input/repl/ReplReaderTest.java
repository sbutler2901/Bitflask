package dev.sbutler.bitflask.client.client_processing.input.repl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplString;
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
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void inlineSingleQuote() throws Exception {
    // Arrange
    String value = "set test val'ue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("val'ue")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void inlineDoubleQuote() throws Exception {
    // Arrange
    String value = "set test val\"ue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("val\"ue")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_withoutBreaks() throws Exception {
    // Arrange
    String value = "set test 'value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_withoutBreaks() throws Exception {
    // Arrange
    String value = "set test \"value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_withSpaces() throws Exception {
    // Arrange
    String value = "set test 'value other'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_withSpaces() throws Exception {
    // Arrange
    String value = "set test \"value other\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_spaceAfter() throws Exception {
    // Arrange
    String value = "'set' 'test' 'value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_spaceAfter() throws Exception {
    // Arrange
    String value = "\"set\" \"test\" \"value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_startOfInput() throws Exception {
    // Arrange
    String value = "'test'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("test")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_startOfInput() throws Exception {
    // Arrange
    String value = "\"test\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("test")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_withEscape_singleQuote() throws Exception {
    // Arrange
    String value = "set test 'value \\'other'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value 'other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_withEscape_doubleQuote() throws Exception {
    // Arrange
    String value = "set test \"value \\\"other\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value \"other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test 'value \\\\other'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value \"other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test \"value \\\\other\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value \\other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_unsupportedEscape() throws Exception {
    // Arrange
    String value = "set test 'value\\nother'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value\\nother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void doubleQuote_unsupportedEscape() throws Exception {
    // Arrange
    String value = "set test \"value\\rother\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value\\rother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void singleQuote_unterminated_endOfInput() throws Exception {
    // Arrange
    String value = "set test 'value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("not properly terminated"));
  }

  @Test
  void doubleQuote_unterminated_endOfInput() throws Exception {
    // Arrange
    String value = "set test \"value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("not properly terminated"));
  }

  @Test
  void singleQuote_unterminated_spaceNotFollowing() throws Exception {
    // Arrange
    String value = "set test 'value'a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("not properly terminated"));
  }

  @Test
  void doubleQuote_unterminated_spaceNotFollowing() throws Exception {
    // Arrange
    String value = "set test \"value\"a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("not properly terminated"));
  }

  @Test
  void doubleQuote_withEscape_newline() throws Exception {
    // Arrange
    String value = "set test \"value\\nother\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value\nother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }
}

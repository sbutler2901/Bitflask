package dev.sbutler.bitflask.client.client_processing.input.repl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplDoubleQuotedString;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplInteger;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplSingleQuotedString;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplString;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class ReplReaderTest {

  @Test
  void readReplString() throws Exception {
    // Arrange
    String value = "test";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplString replString = replReader.readReplString();
    // Assert
    assertThat(replString.getAsString()).isEqualTo(value);
  }

  @Test
  void readReplString_inlineSingleQuote() throws Exception {
    // Arrange
    String value = "te'st";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplString replString = replReader.readReplString();
    // Assert
    assertThat(replString.getAsString()).isEqualTo(value);
  }

  @Test
  void readReplString_inlineDoubleQuote() throws Exception {
    // Arrange
    String value = "te\"st";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplString replString = replReader.readReplString();
    // Assert
    assertThat(replString.getAsString()).isEqualTo(value);
  }

  @Test
  void readReplString_enclosedWithSingleQuotes() throws Exception {
    // Arrange
    String value = "'test'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplString replString = replReader.readReplString();
    // Assert
    assertThat(replString.getAsString()).isEqualTo(value);
  }

  @Test
  void readReplString_enclosedWithDoubleQuotes() throws Exception {
    // Arrange
    String value = "\"test\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplString replString = replReader.readReplString();
    // Assert
    assertThat(replString.getAsString()).isEqualTo(value);
  }

  @Test
  void readReplInteger() throws Exception {
    // Arrange
    Long value = 100L;
    Reader reader = new StringReader(value.toString());
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplInteger replInteger = replReader.readReplInteger();
    // Assert
    assertThat(replInteger.getAsLong()).isEqualTo(value);
  }

  @Test
  void readReplInteger_invalid_throwsReplSyntaxException() {
    // Arrange
    String value = "90a9";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readReplInteger);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("could not be read"));
    assertTrue(exception.getMessage().toLowerCase().contains(value));
  }

  @Test
  void readReplSingleQuotedString() throws Exception {
    // Arrange
    String value = "'test'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSingleQuotedString singleQuotedString = replReader.readReplSingleQuotedString();
    // Assert
    assertThat(singleQuotedString.getAsString()).isEqualTo("test");
  }

  @Test
  void readReplSingleQuotedString_withInlineSpaces() throws Exception {
    // Arrange
    String value = "'test value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSingleQuotedString singleQuotedString = replReader.readReplSingleQuotedString();
    // Assert
    assertThat(singleQuotedString.getAsString()).isEqualTo("test value");
  }

  @Test
  void readReplSingleQuotedString_withEscape_singleQuote() throws Exception {
    // Arrange
    String value = "'test \\'value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSingleQuotedString singleQuotedString = replReader.readReplSingleQuotedString();
    // Assert
    assertThat(singleQuotedString.getAsString()).isEqualTo("test 'value");
  }

  @Test
  void readReplSingleQuotedString_withEscape_backslash() throws Exception {
    // Arrange
    String value = "'test \\\\value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSingleQuotedString singleQuotedString = replReader.readReplSingleQuotedString();
    // Assert
    assertThat(singleQuotedString.getAsString()).isEqualTo("test \\value");
  }

  @Test
  void readReplSingleQuotedString_unsupportedEscape() throws Exception {
    // Arrange
    String value = "'test\\nvalue'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSingleQuotedString singleQuotedString = replReader.readReplSingleQuotedString();
    // Assert
    assertThat(singleQuotedString.getAsString()).isEqualTo("test\\nvalue");
  }

  @Test
  void readReplSingleQuotedString_unterminated_endOfInput_throwsReplSyntaxException() {
    // Arrange
    String value = "'value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("not properly terminated"));
  }

  @Test
  void replSingleQuotedString_spaceNotFollowing_throwsReplSyntaxException() {
    // Arrange
    String value = "'value'a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("followed with a space"));
  }

  @Test
  void readReplDoubleQuotedString() throws Exception {
    // Arrange
    String value = "\"test\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplDoubleQuotedString doubleQuotedString = replReader.readReplDoubleQuotedString();
    // Assert
    assertThat(doubleQuotedString.getAsString()).isEqualTo("test");
  }

  @Test
  void readReplDoubleQuotedString_withInlineSpaces() throws Exception {
    // Arrange
    String value = "\"test value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplDoubleQuotedString doubleQuotedString = replReader.readReplDoubleQuotedString();
    // Assert
    assertThat(doubleQuotedString.getAsString()).isEqualTo("test value");
  }

  @Test
  void readReplDoubleQuotedString_withEscape_doubleQuote() throws Exception {
    // Arrange
    String value = "\"test \\\"value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplDoubleQuotedString doubleQuotedString = replReader.readReplDoubleQuotedString();
    // Assert
    assertThat(doubleQuotedString.getAsString()).isEqualTo("test \"value");
  }

  @Test
  void readReplDoubleQuotedString_withEscape_backslash() throws Exception {
    // Arrange
    String value = "\"test \\\\value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplDoubleQuotedString doubleQuotedString = replReader.readReplDoubleQuotedString();
    // Assert
    assertThat(doubleQuotedString.getAsString()).isEqualTo("test \\value");
  }

  @Test
  void readReplDoubleQuotedString_unsupportedEscape() throws Exception {
    // Arrange
    String value = "\"test\\rvalue\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplDoubleQuotedString doubleQuotedString = replReader.readReplDoubleQuotedString();
    // Assert
    assertThat(doubleQuotedString.getAsString()).isEqualTo("test\\rvalue");
  }

  @Test
  void readReplDoubleQuotedString_unterminated_endOfInput_throwsReplSyntaxException() {
    // Arrange
    String value = "\"value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("not properly terminated"));
  }

  @Test
  void replDoubleQuotedString_spaceNotFollowing_throwsReplSyntaxException() {
    // Arrange
    String value = "\"value\"a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("followed with a space"));
  }

  @Test
  void replDoubleQuotedString_withEscape_newline() throws Exception {
    // Arrange
    String value = "\"test\\nvalue\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplDoubleQuotedString doubleQuotedString = replReader.readReplDoubleQuotedString();
    // Assert
    assertThat(doubleQuotedString.getAsString()).isEqualTo("test\nvalue");
  }

  @Test
  void readToEndLine_spaceSeparated_allStrings() throws Exception {
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
  void readToEndLine_spaceSeparated_withNumber() throws Exception {
    // Arrange
    String value = "set test 1234";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplInteger(1234L)
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_inlineSingleQuote() throws Exception {
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
  void readToEndLine_inlineDoubleQuote() throws Exception {
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
  void readToEndLine_newline() throws Exception {
    // Arrange
    String value = "test\nvalue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements0 = replReader.readToEndLine();
    ImmutableList<ReplElement> elements1 = replReader.readToEndLine();
    // Assert
    assertThat(elements0).hasSize(1);
    assertThat(elements1).hasSize(1);
    ImmutableList<ReplElement> expected0 = ImmutableList.of(
        new ReplString("test")
    );
    ImmutableList<ReplElement> expected1 = ImmutableList.of(
        new ReplString("value")
    );
    assertThat(elements0).containsExactlyElementsIn(expected0);
    assertThat(elements1).containsExactlyElementsIn(expected1);
  }

  @Test
  void readToEndLine_replSingleQuotedString_withoutBreaks() throws Exception {
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
        new ReplSingleQuotedString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_withoutBreaks() throws Exception {
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
        new ReplDoubleQuotedString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replSingleQuotedString_withSpaces() throws Exception {
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
        new ReplSingleQuotedString("value other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_withSpaces() throws Exception {
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
        new ReplDoubleQuotedString("value other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replSingleQuotedString_multiple() throws Exception {
    // Arrange
    String value = "'set' 'test' 'value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplSingleQuotedString("set"),
        new ReplSingleQuotedString("test"),
        new ReplSingleQuotedString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_multiple() throws Exception {
    // Arrange
    String value = "\"set\" \"test\" \"value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplDoubleQuotedString("set"),
        new ReplDoubleQuotedString("test"),
        new ReplDoubleQuotedString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replSingleQuotedString_withEscape_singleQuote() throws Exception {
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
        new ReplSingleQuotedString("value 'other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_withEscape_doubleQuote() throws Exception {
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
        new ReplDoubleQuotedString("value \"other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replSingleQuotedString_withEscape_backslash() throws Exception {
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
        new ReplSingleQuotedString("value \\other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_withEscape_backslash() throws Exception {
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
        new ReplDoubleQuotedString("value \\other")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_withEscape_newline() throws Exception {
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
        new ReplDoubleQuotedString("value\nother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replSingleQuotedString_unsupportedEscape() throws Exception {
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
        new ReplSingleQuotedString("value\\nother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replDoubleQuotedString_unsupportedEscape() throws Exception {
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
        new ReplDoubleQuotedString("value\\rother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replSingleQuotedString_spaceNotFollowing_throwsReplSyntaxException() {
    // Arrange
    String value = "set test 'value'a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("followed with a space"));
  }

  @Test
  void readToEndLine_replDoubleQuotedString_spaceNotFollowing_throwsReplSyntaxException() {
    // Arrange
    String value = "set test \"value\"a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertTrue(exception.getMessage().toLowerCase().contains("followed with a space"));
  }

  @Test
  void close() throws IOException {
    // Arrange
    Reader reader = mock(Reader.class);
    ReplReader replReader = new ReplReader(reader);
    // Act
    replReader.close();
    // Assert
    verify(reader, times(1)).close();
  }
}

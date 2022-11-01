package dev.sbutler.bitflask.client.client_processing.repl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplInteger;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplString;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ReplReaderTest {

  @Test
  void readToEndLine_endDocument() throws Exception {
    // Arrange
    Reader reader = new StringReader("");
    ReplReader replReader = new ReplReader(reader);
    // Act
    Optional<ImmutableList<ReplElement>> elements = replReader.readToEndLine();
    // Assert
    assertThat(elements.isEmpty()).isTrue();
  }

  @Test
  void readToEndLine_replInteger() throws Exception {
    // Arrange
    String value = "1234";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements).hasSize(1);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplInteger(1234L));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_replInteger_attemptFailed_rawStringReturned() throws Exception {
    // Arrange
    String value = "1abc";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements).hasSize(1);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("1abc"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_rawString_spaceSeparated() throws Exception {
    // Arrange
    String value = "set test value";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_rawString_extraWhitespace() throws Exception {
    // Arrange
    String value = "   set\ttest    value   ";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_rawStringAndNumber_spaceSeparated() throws Exception {
    // Arrange
    String value = "set test 1234";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_rawString_inlineSingleQuote() throws Exception {
    // Arrange
    String value = "val'ue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements).hasSize(1);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("val'ue"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_rawString_inlineDoubleQuote() throws Exception {
    // Arrange
    String value = "set test val\"ue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("val\"ue"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_rawString_newlineBetween() throws Exception {
    // Arrange
    String value = "test\nvalue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements0 = replReader.readToEndLine().orElseThrow();
    ImmutableList<ReplElement> elements1 = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements0).hasSize(1);
    assertThat(elements1).hasSize(1);
    ImmutableList<ReplElement> expected0 = ImmutableList.of(
        new ReplString("test"));
    ImmutableList<ReplElement> expected1 = ImmutableList.of(
        new ReplString("value"));
    assertThat(elements0).containsExactlyElementsIn(expected0);
    assertThat(elements1).containsExactlyElementsIn(expected1);
  }

  @Test
  void readToEndLine_singleQuotedString_withoutBreaks() throws Exception {
    // Arrange
    String value = "set test 'value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_doubleQuotedString_withoutBreaks() throws Exception {
    // Arrange
    String value = "set test \"value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_singleQuotedString_withSpaces() throws Exception {
    // Arrange
    String value = "set test 'value other'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_doubleQuotedString_withSpaces() throws Exception {
    // Arrange
    String value = "set test \"value other\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_singleQuotedString_multiple() throws Exception {
    // Arrange
    String value = "'set' 'test' 'value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_doubleQuotedString_multiple() throws Exception {
    // Arrange
    String value = "\"set\" \"test\" \"value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_singleQuotedString_withEscape_singleQuote() throws Exception {
    // Arrange
    String value = "set test 'value \\'other'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_doubleQuotedString_withEscape_doubleQuote() throws Exception {
    // Arrange
    String value = "set test \"value \\\"other\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_singleQuotedString_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test 'value \\\\other'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_doubleQuotedString_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test \"value \\\\other\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_doubleQuotedString_withEscape_newline() throws Exception {
    // Arrange
    String value = "set test \"value\\nother\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value\nother")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLine_singleQuotedString_unsupportedEscape() throws Exception {
    // Arrange
    String value = "set test 'value\\nother'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_doubleQuotedString_unsupportedEscape() throws Exception {
    // Arrange
    String value = "set test \"value\\rother\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
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
  void readToEndLine_singleQuotedString_endDocumentWithInvalidQuote() {
    // Arrange
    String value = "set test 'value\"";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("not properly terminated");
    assertThat(exception).hasMessageThat().ignoringCase().contains("Ending quote not found");
  }

  @Test
  void readToEndLine_doubleQuotedString_endDocumentWithInvalidQuote() {
    // Arrange
    String value = "set test \"value'";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("not properly terminated");
    assertThat(exception).hasMessageThat().ignoringCase().contains("Ending quote not found");
  }

  @Test
  void readToEndLine_singleQuotedString_spaceNotFollowing_throwsReplSyntaxException() {
    // Arrange
    String value = "set test 'value'a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("not properly terminated");
    assertThat(exception).hasMessageThat().ignoringCase().contains("after end quote");
  }

  @Test
  void readToEndLine_doubleQuotedString_spaceNotFollowing_throwsReplSyntaxException() {
    // Arrange
    String value = "set test \"value\"a";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("not properly terminated");
    assertThat(exception).hasMessageThat().ignoringCase().contains("after end quote");
  }

  @Test
  void readToEndLine_invalidTokenPeeked_throwsReplSyntaxException() {
    // Arrange
    String value = "\\";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("Unexpected backslash found");
  }

  @Test
  void readToEndLine_unmappablePeeked_throwsReplSyntaxException() {
    // Arrange
    String value = "¶";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplSyntaxException exception =
        assertThrows(ReplSyntaxException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("Could not map to ReplToken");
  }

  @Test
  void readToEndLine_readerIOExceptions_throwsReplIOException() throws Exception {
    // Arrange
    Reader reader = mock(Reader.class);
    IOException ioException = new IOException("test");
    when(reader.read()).thenThrow(ioException);
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplIOException exception =
        assertThrows(ReplIOException.class, replReader::readToEndLine);
    // Assert
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat().ignoringCase().contains("while reading input data");
  }

  @Test
  void readToEndLineWithoutParsing() throws Exception {
    // Arrange
    String value = "test\nvalue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    replReader.readToEndLineWithoutParsing();
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Act
    assertThat(elements).hasSize(1);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("value"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readToEndLineWithoutParsing_suppressReplSyntaxException() throws Exception {
    // Arrange
    String value = "¶\nvalue";
    Reader reader = new StringReader(value);
    ReplReader replReader = new ReplReader(reader);
    // Act
    replReader.readToEndLineWithoutParsing();
    ImmutableList<ReplElement> elements = replReader.readToEndLine().orElseThrow();
    // Act
    assertThat(elements).hasSize(1);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("value"));
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void close() throws Exception {
    // Arrange
    Reader reader = mock(Reader.class);
    ReplReader replReader = new ReplReader(reader);
    // Act
    replReader.close();
    // Assert
    verify(reader, times(1)).close();
  }

  @Test
  void close_throwsReplIOException() throws Exception {
    // Arrange
    Reader reader = mock(Reader.class);
    IOException ioException = new IOException("test");
    doThrow(ioException).when(reader).close();
    ReplReader replReader = new ReplReader(reader);
    // Act
    ReplIOException exception =
        assertThrows(ReplIOException.class, replReader::close);
    // Assert
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
    assertThat(exception).hasMessageThat().ignoringCase().contains("closing the reader");
  }
}

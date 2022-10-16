package dev.sbutler.bitflask.client.client_processing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import org.junit.jupiter.api.Test;

public class InputToArgsConverterTest {

  InputToArgsConverter converter = new InputToArgsConverter();

  @Test
  void spaceSeparatedStrings() throws Exception {
    // Arrange
    String value = "set test value";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value", args.get(2));
  }

  @Test
  void singleQuote_withoutBreaks() throws Exception {
    // Arrange
    String value = "set test 'value'";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value", args.get(2));
  }

  @Test
  void doubleQuote_withoutBreaks() throws Exception {

    // Arrange
    String value = "set test \"value\"";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value", args.get(2));
  }

  @Test
  void singleQuote_withSpaces() throws Exception {
    // Arrange
    String value = "set test 'value other'";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value other", args.get(2));
  }

  @Test
  void doubleQuote_withSpaces() throws Exception {
    // Arrange
    String value = "set test \"value other\"";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value other", args.get(2));
  }

  @Test
  void singleQuote_withEscape_singleQuote() throws Exception {
    // Arrange
    String value = "set test 'value \\'other'";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value 'other", args.get(2));
  }

  @Test
  void singleQuote_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test 'value \\\\other'";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value \\other", args.get(2));
  }

  @Test
  void singleQuote_withNewline() throws Exception {
    // Arrange
    String value = "set test 'value\nother'";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value\nother", args.get(2));
  }

  @Test
  void doubleQuote_withEscape_singleQuote() throws Exception {
    // Arrange
    String value = "set test \"value \\\"other\"";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value \"other", args.get(2));
  }

  @Test
  void doubleQuote_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test \"value \\\\other\"";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value \\other", args.get(2));
  }

  @Test
  void doubleQuote_withEscape_newline() throws Exception {
    // Arrange
    String value = "set test \"value\\nother\"";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value\nother", args.get(2));
  }

  @Test
  void singleQuote_parseException_builderNotEmpty() {
    // Arrange
    String value = "set test a'value other'";
    // Act
    ParseException e =
        assertThrows(ParseException.class, () -> converter.convert(value));
    // Assert
    assertTrue(e.getMessage().toLowerCase().contains("quoted string"));
  }

  @Test
  void doubleQuote_parseException_builderNotEmpty() {
    // Arrange
    String value = "set test a\"value other\"";
    // Act
    ParseException e =
        assertThrows(ParseException.class, () -> converter.convert(value));
    // Assert
    assertTrue(e.getMessage().toLowerCase().contains("quoted string"));
  }
}

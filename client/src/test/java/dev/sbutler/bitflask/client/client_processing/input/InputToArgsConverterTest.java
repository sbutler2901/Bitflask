package dev.sbutler.bitflask.client.client_processing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

public class InputToArgsConverterTest {

  InputToArgsConverter converter = new InputToArgsConverter();

  @Test
  void spaceSeparatedStrings() {
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
  void singleQuote_withoutBreaks() {
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
  void doubleQuote_withoutBreaks() {

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
  void singleQuote_withSpaces() {
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
  void doubleQuote_withSpaces() {
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
  void singleQuote_withEscape_singleQuote() {
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
  void singleQuote_withEscape_backslash() {
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
  void doubleQuote_withEscape_singleQuote() {
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
  void doubleQuote_withEscape_backslash() {
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
  void doubleQuote_withEscape_newline() {
    // Arrange
    String value = "set test \"value\\nother\"";
    // Act
    ImmutableList<String> args = converter.convert(value);
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value\\nother", args.get(2));
  }
}

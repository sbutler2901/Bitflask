package dev.sbutler.bitflask.client.client_processing.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

public class InputToArgsConverterTest {

  @Test
  void spaceSeparatedStrings() throws Exception {
    // Arrange
    String value = "set test value";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value", args.get(2));
  }

  @Test
  void inlineSingleQuote() throws Exception {
    // Arrange
    String value = "set test val'ue";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("val'ue", args.get(2));
  }

  @Test
  void inlineDoubleQuote() throws Exception {
    // Arrange
    String value = "set test val\"ue";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("val\"ue", args.get(2));
  }

  @Test
  void singleQuote_withoutBreaks() throws Exception {
    // Arrange
    String value = "set test 'value'";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
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
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
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
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
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
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
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
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value 'other", args.get(2));
  }

  @Test
  void doubleQuote_withEscape_doubleQuote() throws Exception {
    // Arrange
    String value = "set test \"value \\\"other\"";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value \"other", args.get(2));
  }

  @Test
  void singleQuote_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test 'value \\\\other'";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value \\other", args.get(2));
  }

  @Test
  void doubleQuote_withEscape_backslash() throws Exception {
    // Arrange
    String value = "set test \"value \\\\other\"";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value \\other", args.get(2));
  }

  @Test
  void singleQuote_unsupportedEscape() throws Exception {
    // Arrange
    String value = "set test 'value\\nother'";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value\\nother", args.get(2));
  }

  @Test
  void doubleQuote_unsupportedEscape() throws Exception {
    // Arrange
    String value = "set test \"value\\rother\"";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value\\rother", args.get(2));
  }

  @Test
  void doubleQuote_withEscape_newline() throws Exception {
    // Arrange
    String value = "set test \"value\\nother\"";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ImmutableList<String> args = converter.convert();
    // Assert
    assertEquals(3, args.size());
    assertEquals("set", args.get(0));
    assertEquals("test", args.get(1));
    assertEquals("value\nother", args.get(2));
  }
/*
  @Test
  void singleQuote_parseException_builderNotEmpty() {
    // Arrange
    String value = "set test a'value other'";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ParseException e =
        assertThrows(ParseException.class, converter::convert);
    // Assert
    assertTrue(e.getMessage().toLowerCase().contains("quoted string"));
  }

  @Test
  void doubleQuote_parseException_builderNotEmpty() {
    // Arrange
    String value = "set test a\"value other\"";
    InputToArgsConverter converter = new InputToArgsConverter(value);
    // Act
    ParseException e =
        assertThrows(ParseException.class, converter::convert);
    // Assert
    assertTrue(e.getMessage().toLowerCase().contains("quoted string"));
  }*/
}
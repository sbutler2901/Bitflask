package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.util.function.BiConsumer;

/**
 * Handles converting client input into discrete arguments.
 *
 * <p>This includes double and single quoted strings with character escaping.
 */
public class InputToArgsConverter {

  public ImmutableList<String> convert(String value) throws ParseException {
    return parseIntoArgs(value.trim().toCharArray());
  }

  private ImmutableList<String> parseIntoArgs(char[] chars) throws ParseException {
    ImmutableList.Builder<String> args = ImmutableList.builder();

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < chars.length; ) {
      char current = chars[i];
      if (current == '"') {
        verifyValidToParseQuotedString(builder, i);
        ParsedQuotedString parsed =
            parsedQuotedString(chars, i, '"', this::doubleQuoteHandleEscape);
        args.add(parsed.arg());
        i = parsed.nextIndex();
      } else if (current == '\'') {
        verifyValidToParseQuotedString(builder, i);
        ParsedQuotedString parsed =
            parsedQuotedString(chars, i, '\'', this::singleQuoteHandleEscape);
        args.add(parsed.arg());
        i = parsed.nextIndex();
      } else {
        if (current == ' ') {
          // Start next arg
          args.add(builder.toString());
          builder.setLength(0);
        } else {
          // add to current arg
          builder.append(current);
        }
        i++;
      }
    }
    // Handle reaching end of string
    if (!builder.isEmpty()) {
      args.add(builder.toString());
    }

    return args.build();
  }

  /**
   * Parses a quoted string.
   *
   * @param chars          the array of chars from which the quoted string should be parsed
   * @param startIndex     the index of the first, starting quote character
   * @param quote          the quoting character starting and terminating the string being parsed
   * @param handleEscaping a function specific to a quote type for handling the current character
   *                       after an escape has been found
   * @return the quoted string parsed
   */
  private ParsedQuotedString parsedQuotedString(char[] chars, int startIndex, char quote,
      BiConsumer<StringBuilder, Character> handleEscaping) {
    StringBuilder builder = new StringBuilder();
    int i;
    boolean escapeActive = false;
    for (i = startIndex + 1; i < chars.length; i++) {
      char current = chars[i];
      if (escapeActive) {
        handleEscaping.accept(builder, current);
        escapeActive = false;
      } else if (current == quote) {
        // quote complete
        i++;
        break;
      } else if (current == '\\') {
        // start escape for next char
        escapeActive = true;
      } else {
        builder.append(current);
      }
    }
    return new ParsedQuotedString(builder.toString(), i);
  }

  private void doubleQuoteHandleEscape(StringBuilder builder, char current) {
    switch (current) {
      case '\\', '"' -> builder.append(current);
      case 'n' -> builder.append('\n');
      default -> {
        // Unsupported escape, include backslash
        builder.append('\\');
        builder.append(current);
      }
    }
  }

  private void singleQuoteHandleEscape(StringBuilder builder, char current) {
    switch (current) {
      case '\\', '\'' -> builder.append(current);
      default -> {
        // Unsupported escape, include backslash
        builder.append('\\');
        builder.append(current);
      }
    }
  }

  private void verifyValidToParseQuotedString(StringBuilder builder, int index)
      throws ParseException {
    if (!builder.isEmpty()) {
      throw new ParseException("A quoted string can only be parsed when preceded with a space",
          index);
    }
  }

  private record ParsedQuotedString(String arg, int nextIndex) {

  }
}

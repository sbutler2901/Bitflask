package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.util.function.Function;

/**
 * Handles converting client input into discrete arguments.
 *
 * <p>This includes double and single quoted strings with character escaping.
 */
public class InputToArgsConverter {

  private final char[] input;

  InputToArgsConverter(String input) {
    this.input = input.trim().toCharArray();
  }

  /**
   * Convert the input provided by the client into discrete arguments
   */
  public ImmutableList<String> convert() throws ParseException {
    ImmutableList.Builder<String> args = ImmutableList.builder();

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < input.length; ) {
      char current = input[i];
      if (current == '"' || current == '\'') {
        // Parse quoted string
        verifyValidToParseQuotedString(builder, i);
        ParsedQuotedString parsed = parseQuotedString(i);
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
   * Parses a quoted string. The provided {@code startIndex} should be the quote character that
   * starts the quoted String.
   */
  private ParsedQuotedString parseQuotedString(int startIndex) {
    char quote = input[startIndex];
    Function<Character, String> escapeHandler = quote == '"'
        ? InputToArgsConverter::doubleQuoteEscapeHandler
        : InputToArgsConverter::singleQuoteEscapeHandler;

    StringBuilder builder = new StringBuilder();
    int i;
    boolean escapeActive = false;
    for (i = startIndex + 1; i < input.length; i++) {
      char current = input[i];
      if (escapeActive) {
        String result = escapeHandler.apply(current);
        builder.append(result);
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

  /**
   * Used to determine the result of escaping the current character in a double-quoted string.
   */
  private static String doubleQuoteEscapeHandler(char current) {
    return switch (current) {
      case '\\', '"' -> String.valueOf(current);
      case 'n' -> "\n";
      // Unsupported escape, include backslash
      default -> "\\" + current;
    };
  }

  /**
   * Used to determine the result of escaping the current character in a single-quoted string.
   */
  private static String singleQuoteEscapeHandler(char current) {
    return switch (current) {
      case '\\', '\'' -> String.valueOf(current);
      // Unsupported escape, include backslash
      default -> "\\" + current;
    };
  }

  /**
   * Used to ensure in a valid state for parsing a quoted string.
   */
  private static void verifyValidToParseQuotedString(StringBuilder builder, int index)
      throws ParseException {
    if (!builder.isEmpty()) {
      throw new ParseException("A quoted string can only be parsed when preceded with a space",
          index);
    }
  }

  private record ParsedQuotedString(String arg, int nextIndex) {

  }
}

package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;
import java.util.function.Function;

/**
 * Handles converting client input into discrete arguments.
 *
 * <p>This includes double and single quoted strings with character escaping.
 */
class InputToArgsConverter {

  private static final char SINGLE_QUOTE = '\'';
  private static final char DOUBLE_QUOTE = '"';
  private static final char SINGLE_SPACE = ' ';
  private static final char BACKSLASH = '\\';

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
    for (int i = 0; i < input.length; i++) {
      char current = input[i];
      if ((current == DOUBLE_QUOTE || current == SINGLE_QUOTE) && isValidToParseQuotedString(i)) {
        // Parse quoted string
        ParsedQuotedString parsed = parseQuotedString(i);
        args.add(parsed.arg());
        i = parsed.lastIndex();
      } else if (current == SINGLE_SPACE) {
        // Start next arg
        args.add(builder.toString());
        builder.setLength(0);
      } else {
        // add to current arg
        builder.append(current);
      }
    }
    // Handle reaching end of string
    if (!builder.isEmpty()) {
      args.add(builder.toString());
    }

    return args.build();
  }

  /**
   * Used to ensure in a valid state for parsing a quoted string.
   */
  private boolean isValidToParseQuotedString(int currentIndex) {
    if (currentIndex == 0) {
      return true;
    }
    return input[currentIndex - 1] == SINGLE_SPACE;
  }

  /**
   * Parses a quoted string. The provided {@code startIndex} should be the quote character that
   * starts the quoted String.
   */
  private ParsedQuotedString parseQuotedString(int startIndex) throws ParseException {
    char quote = input[startIndex];
    Function<Character, String> escapeHandler = quote == DOUBLE_QUOTE
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
        break;
      } else if (current == BACKSLASH) {
        // start escape for next char
        escapeActive = true;
      } else {
        builder.append(current);
      }
    }
    if (i == input.length) {
      throw new ParseException("A quoted input was not properly terminated", i);
    }
    return new ParsedQuotedString(builder.toString(), i);
  }

  /**
   * Used to determine the result of escaping the current character in a double-quoted string.
   */
  private static String doubleQuoteEscapeHandler(char current) {
    return switch (current) {
      case BACKSLASH, DOUBLE_QUOTE -> String.valueOf(current);
      case 'n' -> "\n";
      // Unsupported escape, include backslash
      default -> String.valueOf(BACKSLASH) + current;
    };
  }

  /**
   * Used to determine the result of escaping the current character in a single-quoted string.
   */
  private static String singleQuoteEscapeHandler(char current) {
    return switch (current) {
      case BACKSLASH, SINGLE_QUOTE -> String.valueOf(current);
      // Unsupported escape, include backslash
      default -> String.valueOf(BACKSLASH) + current;
    };
  }

  private record ParsedQuotedString(String arg, int lastIndex) {

  }
}

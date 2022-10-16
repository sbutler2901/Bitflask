package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;
import java.text.ParseException;

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
        ParsedQuotedString parsed = parseDoubleQuotedString(chars, i);
        args.add(parsed.arg());
        i = parsed.nextIndex();
      } else if (current == '\'') {
        verifyValidToParseQuotedString(builder, i);
        ParsedQuotedString parsed = parseSingleQuotedString(chars, i);
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
   * Parses a double-quoted string. The provided {@code startIndex} should be the index of the
   * first, starting double quote.
   */
  private ParsedQuotedString parseDoubleQuotedString(char[] chars, int startIndex) {
    StringBuilder builder = new StringBuilder();
    int i;
    boolean escapeActive = false;
    for (i = startIndex + 1; i < chars.length; i++) {
      char current = chars[i];
      if (escapeActive) {
        if (current == 'n') {
          builder.append("\\");
        }
        builder.append(current);
        escapeActive = false;
      } else if (current == '"') {
        i++;
        break;
      } else if (current == '\\') {
        escapeActive = true;
      } else {
        builder.append(current);
      }
    }
    return new ParsedQuotedString(builder.toString(), i);
  }

  /**
   * Parses a single-quoted string. The provided {@code startIndex} should be the index of the
   * first, starting double quote.
   */
  private ParsedQuotedString parseSingleQuotedString(char[] chars, int startIndex) {
    StringBuilder builder = new StringBuilder();
    int i;
    boolean escapeActive = false;
    for (i = startIndex + 1; i < chars.length; i++) {
      char current = chars[i];
      if (escapeActive) {
        builder.append(current);
        escapeActive = false;
      } else if (current == '\'') {
        i++;
        break;
      } else if (current == '\\') {
        escapeActive = true;
      } else {
        builder.append(current);
      }
    }
    return new ParsedQuotedString(builder.toString(), i);
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

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

    return args.build();
  }

  private ParsedQuotedString parseDoubleQuotedString(char[] chars, int startIndex) {
    return new ParsedQuotedString("", 0);
  }

  private ParsedQuotedString parseSingleQuotedString(char[] chars, int startIndex) {
    return new ParsedQuotedString("", 0);
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

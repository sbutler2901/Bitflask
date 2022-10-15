package dev.sbutler.bitflask.client.client_processing.input;

import com.google.common.collect.ImmutableList;

public class InputToArgsConverter {

  public ImmutableList<String> convert(String value) {
    return parseIntoArgs(value.trim().toCharArray());
  }

  private ImmutableList<String> parseIntoArgs(char[] chars) {
    ImmutableList.Builder<String> args = ImmutableList.builder();

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < chars.length; ) {
      char current = chars[i];
      if (current == '"') {
        ParsedQuotedString parsed = parseDoubleQuotedString(chars, i);
        args.add(parsed.arg());
        i = parsed.nextIndex();
      } else if (current == '\'') {
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

  private record ParsedQuotedString(String arg, int nextIndex) {

  }
}

package dev.sbutler.bitflask.client.client_processing.input;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;

public class InputToArgsConverter {

  private static final String SPACE_REGEX = "\\s+";

  public ImmutableList<String> convert(String value) {
    return Arrays.stream(value.split(SPACE_REGEX))
        .filter((arg) -> !arg.isBlank()).collect(toImmutableList());
//    return parseIntoArgs(value.trim().toCharArray());
  }

  private ImmutableList<String> parseIntoArgs(char[] chars) {
    ImmutableList.Builder<String> args = ImmutableList.builder();

    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < chars.length; i++) {
      char current = chars[i];
      if (current == ' ') {
        // Start next arg
        args.add(builder.toString());
        builder.setLength(0);
      } else if (current == '"') {
      } else {
        // add to current arg
        builder.append(current);
      }
    }

    return args.build();
  }

  private ParsedDoubleQuotedString parseDoubleQuotedString() {
    return new ParsedDoubleQuotedString("", 0);
  }

  private record ParsedDoubleQuotedString(String arg, int nextIndex) {

  }
}

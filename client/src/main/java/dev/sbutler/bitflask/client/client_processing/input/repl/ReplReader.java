package dev.sbutler.bitflask.client.client_processing.input.repl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplDoubleQuotedString;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplInteger;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplSingleQuotedString;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplString;
import java.io.IOException;
import java.io.Reader;

public final class ReplReader implements AutoCloseable {

  private final Reader reader;

  private int peeked;
  private ReplToken peekedAsToken = null;

  public ReplReader(Reader reader) {
    this.reader = reader;
  }

  private void peek() throws IOException {
    if (peekedAsToken == ReplToken.END_DOCUMENT) {
      return;
    }
    peeked = reader.read();
    peekedAsToken = mapToToken(peeked);
  }

  ReplString readString() throws IOException {
    StringBuilder builder = new StringBuilder();
    readAllWhiteSpace();
    while (!isPeekedSpace() || peekedIsNotEnd()) {
      builder.append(Character.toString(peeked));
      peek();
    }
    return new ReplString(builder.toString());
  }

  @SuppressWarnings("UnstableApiUsage")
  ReplInteger readInteger() throws IOException {
    ReplString replString = readString();
    Long parsed = Longs.tryParse(replString.getAsString());
    if (parsed == null) {
      throw new ReplSyntaxException(
          String.format("A Repl Integer could not be read: [%s]", replString.getAsString()));
    }
    return new ReplInteger(parsed);
  }

  ReplSingleQuotedString readReplSingleQuotedString() throws IOException {
    readAllWhiteSpace();
    return new ReplSingleQuotedString("");
  }

  ReplDoubleQuotedString readReplDoubleQuotedString() throws IOException {
    readAllWhiteSpace();
    return new ReplDoubleQuotedString("");
  }

  @SuppressWarnings("UnstableApiUsage")
  ImmutableList<ReplElement> readToEndLine() throws IOException {
    ImmutableList.Builder<ReplElement> builder = new Builder<>();
    readAllWhiteSpace();
    while (peekedIsNotEnd()) {
      ReplElement element =
          switch (peekedAsToken) {
            case SINGLE_QUOTE -> readReplSingleQuotedString();
            case DOUBLE_QUOTE -> readReplDoubleQuotedString();
            case CHARACTER -> readString();
            case NUMBER -> {
              // Try to read as number
              ReplString replString = readString();
              Long parsed = Longs.tryParse(replString.getAsString());
              if (parsed == null) {
                // was not a number
                yield replString;
              }
              yield new ReplInteger(parsed);
            }
            default -> throw new IllegalStateException("Invalid token found: " + peekedAsToken);
          };
      builder.add(element);
    }
    return builder.build();
  }

  private void readAllWhiteSpace() throws IOException {
    while (isPeekedSpace() && peekedIsNotEnd()) {
      peek();
    }
  }

  private boolean peekedIsNotEnd() {
    return !isPeekedEndLine() && !isPeekedEndDocument();
  }

  private boolean isPeekedCharacter() {
    return peekedAsToken == ReplToken.CHARACTER;
  }

  private boolean isPeekedNumber() {
    return peekedAsToken == ReplToken.NUMBER;
  }

  private boolean isPeekedSingleQuote() {
    return peekedAsToken == ReplToken.SINGLE_QUOTE;
  }

  private boolean isPeekedDoubleQuote() {
    return peekedAsToken == ReplToken.DOUBLE_QUOTE;
  }

  private boolean isPeekedBackSlash() {
    return peekedAsToken == ReplToken.BACK_SLASH;
  }

  private boolean isPeekedSpace() {
    return peekedAsToken == ReplToken.SPACE;
  }

  private boolean isPeekedEndLine() {
    return peekedAsToken == ReplToken.END_LINE;
  }

  private boolean isPeekedEndDocument() {
    return peekedAsToken == ReplToken.END_DOCUMENT;
  }

  private static ReplToken mapToToken(int read) {
    if (read == -1) {
      return ReplToken.END_DOCUMENT;
    }
    if (Character.isAlphabetic(read)) {
      return ReplToken.CHARACTER;
    }
    if (Character.isDigit(read)) {
      return ReplToken.NUMBER;
    }
    if (Character.isSpaceChar(read)) {
      return ReplToken.SPACE;
    }
    String asString = Character.toString(read);
    return switch (asString) {
      case "'" -> ReplToken.SINGLE_QUOTE;
      case "\"" -> ReplToken.DOUBLE_QUOTE;
      case "\\" -> ReplToken.BACK_SLASH;
      case "\n" -> ReplToken.END_LINE;
      default -> throw new ReplSyntaxException(
          String.format("Could not map to ReplToken: int [%d], string [%s]", read, asString));
    };
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}

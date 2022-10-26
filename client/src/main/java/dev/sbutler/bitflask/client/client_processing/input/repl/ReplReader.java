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
import java.util.function.Supplier;

public final class ReplReader implements AutoCloseable {

  /**
   * Various special chars used by the Repl parser for easy reuse.
   */
  private static class SpecialChars {

    static final String SINGLE_QUOTE = "'";
    static final String DOUBLE_QUOTE = "\"";
    static final String BACK_SLASH = "\\";
    static final String NEW_LINE = "\n";
  }

  private final Reader reader;

  private String peeked;
  private ReplToken peekedAsToken = null;

  public ReplReader(Reader reader) {
    this.reader = reader;
  }

  private void peek() throws IOException {
    if (peekedAsToken == ReplToken.END_DOCUMENT) {
      return;
    }
    int read = reader.read();
    peekedAsToken = mapToToken(read);
    peeked = Character.toString(read);
  }

  ReplString readString() throws IOException {
    StringBuilder builder = new StringBuilder();
    readAllWhiteSpaceBeforeNextElement();
    while (shouldContinueParsingElement()) {
      builder.append(peeked);
      peek();
    }
    return new ReplString(builder.toString());
  }

  @SuppressWarnings("UnstableApiUsage")
  ReplInteger readInteger() throws IOException {
    ReplElement readElement = attemptReadingInteger();
    if (readElement instanceof ReplString replString) {
      throw new ReplSyntaxException(
          String.format("A Repl Integer could not be read: [%s]", replString.getAsString()));
    }
    return (ReplInteger) readElement;
  }

  /**
   * Attempts to read the next element as a ReplInteger, returning a ReplString of the value read if
   * not.
   */
  @SuppressWarnings("UnstableApiUsage")
  private ReplElement attemptReadingInteger() throws IOException {
    // Try to read as number
    ReplString replString = readString();
    Long parsed = Longs.tryParse(replString.getAsString());
    if (parsed == null) {
      // was not a number
      return replString;
    }
    return new ReplInteger(parsed);
  }

  ReplSingleQuotedString readReplSingleQuotedString() throws IOException {
    readAllWhiteSpaceBeforeNextElement();
    String quotedString = parseQuotedString();
    return new ReplSingleQuotedString(quotedString);
  }

  ReplDoubleQuotedString readReplDoubleQuotedString() throws IOException {
    readAllWhiteSpaceBeforeNextElement();
    String quotedString = parseQuotedString();
    return new ReplDoubleQuotedString(quotedString);
  }

  ImmutableList<ReplElement> readToEndLine() throws IOException {
    ImmutableList.Builder<ReplElement> builder = new Builder<>();
    while (peekedIsNotEnd()) {
      ReplElement element =
          switch (peekedAsToken) {
            case SINGLE_QUOTE -> readReplSingleQuotedString();
            case DOUBLE_QUOTE -> readReplDoubleQuotedString();
            case CHARACTER -> readString();
            case NUMBER -> attemptReadingInteger();
            default -> throw new IllegalStateException("Invalid token found: " + peekedAsToken);
          };
      builder.add(element);
    }
    return builder.build();
  }

  private String parseQuotedString() throws IOException {
    ReplToken startQuote = peekedAsToken;
    Supplier<String> escapeHandler = startQuote == ReplToken.DOUBLE_QUOTE
        ? this::doubleQuoteEscapeHandler
        : this::singleQuoteEscapeHandler;

    peek(); // consume start quote

    StringBuilder builder = new StringBuilder();
    boolean escapeActive = false;
    while (shouldContinueParsingElement()) {
      if (escapeActive) {
        String result = escapeHandler.get();
        builder.append(result);
        escapeActive = false;
      } else if (peekedAsToken == startQuote) {
        // quote complete
        break;
      } else if (isPeekedBackSlash()) {
        // start escape for next char
        escapeActive = true;
      } else {
        builder.append(peeked);
      }
    }
    if (peekedAsToken != startQuote) {
      throw new ReplSyntaxException("Quoted element was not properly terminated");
    }
    return builder.toString();
  }

  private String doubleQuoteEscapeHandler() {
    if (isPeekedBackSlash() || isPeekedDoubleQuote()) {
      return peeked;
    }
    if (peeked.equals("n")) {
      return SpecialChars.NEW_LINE;
    }
    // Unsupported escape, include backslash
    return SpecialChars.BACK_SLASH + peeked;
  }

  private String singleQuoteEscapeHandler() {
    if (isPeekedBackSlash() || isPeekedSingleQuote()) {
      return peeked;
    }
    // Unsupported escape, include backslash
    return SpecialChars.BACK_SLASH + peeked;
  }

  private void readAllWhiteSpaceBeforeNextElement() throws IOException {
    // Ensure there is at least a single whitespace
    if (!isPeekedSpace() && peekedIsNotEnd()) {
      throw new ReplSyntaxException("Whitespace expected before parsing next element");
    }
    while (isPeekedSpace() && peekedIsNotEnd()) {
      peek();
    }
  }

  private boolean shouldContinueParsingElement() {
    return !isPeekedSpace() || peekedIsNotEnd();
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
      case SpecialChars.SINGLE_QUOTE -> ReplToken.SINGLE_QUOTE;
      case SpecialChars.DOUBLE_QUOTE -> ReplToken.DOUBLE_QUOTE;
      case SpecialChars.BACK_SLASH -> ReplToken.BACK_SLASH;
      case SpecialChars.NEW_LINE -> ReplToken.END_LINE;
      default -> throw new ReplSyntaxException(
          String.format("Could not map to ReplToken: int [%d], string [%s]", read, asString));
    };
  }

  @Override
  public void close() throws IOException {
    reader.close();
  }
}

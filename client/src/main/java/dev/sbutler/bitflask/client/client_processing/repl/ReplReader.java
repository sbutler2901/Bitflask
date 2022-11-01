package dev.sbutler.bitflask.client.client_processing.repl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.primitives.Longs;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplInteger;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplString;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;
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

  private String peeked = "";
  private ReplToken peekedAsToken = ReplToken.START_DOCUMENT;

  public ReplReader(Reader reader) {
    this.reader = reader;
  }

  @Override
  public void close() throws ReplIOException {
    try {
      reader.close();
    } catch (IOException e) {
      throw new ReplIOException("Failed closing the reader", e);
    }
  }

  /**
   * Reads input data until the end of line or input data has been reached.
   *
   * <p>Any {@link ReplSyntaxException}s encountered will be suppressed.
   */
  void readToEndLineWithoutParsing() throws ReplIOException {
    while (isNotEnd()) {
      try {
        peek();
      } catch (ReplSyntaxException e) {
        // Potential syntax issues before reaching the end of line or document should be ignored.
      }
    }
  }

  /**
   * Reads the next line of Repl input data.
   *
   * <p>The returned optional will be empty when the end of the input data has been reached.
   *
   * <p>The list will be empty when there was no data prior to the next end line
   */
  Optional<ImmutableList<ReplElement>> readToEndLine() throws ReplIOException, ReplSyntaxException {
    peek();

    ImmutableList.Builder<ReplElement> builder = new Builder<>();
    while (peekedIsNotEndLine() && peekedIsNotEndDocument()) {
      if (isPeekedSpace()) {
        // Prepare for next iteration
        peek();
        continue;
      }
      if (isPeekedBackSlash()) {
        throw new ReplSyntaxException("Unexpected backslash found while reading");
      }

      ReplElement readReplElement;
      if (isPeekedNumber()) {
        readReplElement = attemptReadingReplInteger();
      } else if (isPeekedSingleQuote()) {
        readReplElement = readSingleQuotedString();
      } else if (isPeekedDoubleQuote()) {
        readReplElement = readDoubleQuotedString();
      } else {
        readReplElement = readRawString();
      }
      builder.add(readReplElement);
      // Prepare for next iteration if end of line has not been reached
      if (peekedIsNotEndLine()) {
        peek();
      }
    }

    ImmutableList<ReplElement> readElements = builder.build();
    if (readElements.isEmpty() && isPeekedEndDocument()) {
      // Called after reaching end of input data
      return Optional.empty();
    }
    return Optional.of(readElements);
  }

  private void peek() throws ReplIOException, ReplSyntaxException {
    int nextPeeked;
    try {
      nextPeeked = reader.read();
    } catch (IOException e) {
      throw new ReplIOException("An error occurred while reading input data", e);
    }

    peekedAsToken = mapNextPeekedToToken(nextPeeked);
    if (isPeekedEndDocument()) {
      peeked = "";
      return;
    }

    peeked = Character.toString(nextPeeked);
  }

  /**
   * Reads the next element as a raw string.
   */
  private ReplString readRawString() throws ReplIOException, ReplSyntaxException {
    StringBuilder builder = new StringBuilder();
    while (shouldContinueReadingElement()) {
      builder.append(peeked);
      peek();
    }
    return new ReplString(builder.toString());
  }

  /**
   * Attempts to read the next element as a ReplInteger, returning a ReplString of the value read if
   * not.
   */
  @SuppressWarnings("UnstableApiUsage")
  private ReplElement attemptReadingReplInteger() throws ReplIOException, ReplSyntaxException {
    // Try to read as number
    ReplString replString = readRawString();
    Long parsed = Longs.tryParse(replString.getAsString());
    if (parsed == null) {
      // was not a number
      return replString;
    }
    return new ReplInteger(parsed);
  }

  private ReplString readSingleQuotedString() throws ReplIOException, ReplSyntaxException {
    String parsedString = parseQuotedString(ReplToken.SINGLE_QUOTE, this::singleQuoteEscapeHandler);
    return new ReplString(parsedString);
  }

  private ReplString readDoubleQuotedString() throws ReplIOException, ReplSyntaxException {
    String parsedString = parseQuotedString(ReplToken.DOUBLE_QUOTE, this::doubleQuoteEscapeHandler);
    return new ReplString(parsedString);
  }

  /**
   * Algorithm to parse a quoted String.
   *
   * <p>Assumes that the starting quote has not been consumed prior to calling this function. The
   * final quote will be consumed before returning.
   *
   * <p>A {@link ReplSyntaxException} will be thrown if the quoted string is not properly
   * terminated.
   *
   * @param startQuote    the quote type to determine which the quoted string has been terminated.
   * @param escapeHandler used for handling the quote's specific escaping
   */
  private String parseQuotedString(ReplToken startQuote, Supplier<String> escapeHandler)
      throws ReplIOException, ReplSyntaxException {
    // consume start quote
    peek();

    StringBuilder builder = new StringBuilder();
    boolean escapeActive = false;
    for (; isNotEnd(); peek()) {
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
      throw new ReplSyntaxException(
          "Quoted element was not properly terminated. Ending quote not found.");
    }

    // Consume end quote
    peek();
    if (shouldContinueReadingElement()) {
      throw new ReplSyntaxException(String.format(
          "Quoted element was not properly terminated. Found [%s] after end quote.", peeked));
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

  private static ReplToken mapNextPeekedToToken(int nextPeeked) throws ReplSyntaxException {
    if (nextPeeked == -1) {
      return ReplToken.END_DOCUMENT;
    }

    if (Character.isAlphabetic(nextPeeked)) {
      return ReplToken.CHARACTER;
    }
    if (Character.isDigit(nextPeeked)) {
      return ReplToken.NUMBER;
    }

    String asString = Character.toString(nextPeeked);
    if (Character.isWhitespace(nextPeeked)) {
      if (asString.equals(SpecialChars.NEW_LINE)) {
        return ReplToken.END_LINE;
      }
      return ReplToken.SPACE;
    }

    return switch (asString) {
      case SpecialChars.SINGLE_QUOTE -> ReplToken.SINGLE_QUOTE;
      case SpecialChars.DOUBLE_QUOTE -> ReplToken.DOUBLE_QUOTE;
      case SpecialChars.BACK_SLASH -> ReplToken.BACK_SLASH;
      // TODO: improve handling of unexpected values
      default -> throw new ReplSyntaxException(
          String.format("Could not map to ReplToken: int [%d], string [%s]", nextPeeked, asString));
    };
  }

  /**
   * Checks if a space, end of the line or document has been reached.
   */
  private boolean shouldContinueReadingElement() {
    return peekedIsNotSpace() && isNotEnd();
  }

  /**
   * Checks that the end of the line or document has not been reached.
   */
  private boolean isNotEnd() {
    return peekedIsNotEndLine() && peekedIsNotEndDocument();
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

  private boolean peekedIsNotSpace() {
    return !isPeekedSpace();
  }

  private boolean isPeekedEndDocument() {
    return peekedAsToken == ReplToken.END_DOCUMENT;
  }

  private boolean peekedIsNotEndDocument() {
    return !isPeekedEndDocument();
  }

  private boolean isPeekedEndLine() {
    return peekedAsToken == ReplToken.END_LINE;
  }

  private boolean peekedIsNotEndLine() {
    return !isPeekedEndLine();
  }
}

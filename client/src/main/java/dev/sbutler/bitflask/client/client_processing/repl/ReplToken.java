package dev.sbutler.bitflask.client.client_processing.repl;

/**
 * Represents the various types of tokens recognized in Repl input data.
 */
public enum ReplToken {
  NUMBER,
  CHARACTER,
  SINGLE_QUOTE,
  DOUBLE_QUOTE,
  BACK_SLASH,
  SPACE,
  START_DOCUMENT,
  END_DOCUMENT,
  END_LINE,
}

package dev.sbutler.bitflask.server.command_processing_service;

class InvalidCommandArgumentsException extends RuntimeException {

  InvalidCommandArgumentsException(String message) {
    super(message);
  }
}

package dev.sbutler.bitflask.client.repl;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;

public interface InputParser {

  ClientCommand getNextCommand();

}

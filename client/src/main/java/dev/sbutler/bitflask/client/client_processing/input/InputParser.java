package dev.sbutler.bitflask.client.client_processing.input;

import dev.sbutler.bitflask.client.command_processing.ClientCommand;

public interface InputParser {

  ClientCommand getNextCommand();

}

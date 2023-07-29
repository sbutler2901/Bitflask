package dev.sbutler.bitflask.server.command_processing_service;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link PingCommand}. */
public class PingCommandTest {

  @Test
  void execute() {
    PingCommand pingCommand = new PingCommand();

    String result = pingCommand.execute();
    assertThat(result).isEqualTo("pong");
  }
}

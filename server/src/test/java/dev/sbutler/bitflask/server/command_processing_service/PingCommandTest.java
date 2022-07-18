package dev.sbutler.bitflask.server.command_processing_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.Test;

public class PingCommandTest {

  @Test
  void execute() throws Exception {
    // Arrange
    PingCommand pingCommand = new PingCommand();
    // Act
    ListenableFuture<String> responseFuture = pingCommand.execute();
    // Assert
    assertTrue(responseFuture.isDone());
    assertEquals("pong", responseFuture.get());
  }
}

package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConnectionManagerTest {

  ConnectionManager connectionManager;
  Socket socket;

  @BeforeEach
  void beforeEach() throws Exception {
    socket = mock(Socket.class);
    connectionManager = new ConnectionManager(socket);
  }

  @Test
  void close() throws Exception {
    // Act
    connectionManager.close();
    // Asset
    verify(socket, times(1)).close();
  }

  @Test
  void getInputStream() throws Exception {
    // Arrange
    InputStream mockInputStream = mock(InputStream.class);
    doReturn(mockInputStream).when(socket).getInputStream();
    // Act
    InputStream inputStream = connectionManager.getInputStream();
    // Assert
    assertEquals(mockInputStream, inputStream);
    verify(socket, times(1)).getInputStream();
  }

  @Test
  void getOutputStream() throws Exception {
    // Arrange
    OutputStream mockOutputStream = mock(OutputStream.class);
    doReturn(mockOutputStream).when(socket).getOutputStream();
    // Act
    OutputStream outputStream = connectionManager.getOutputStream();
    // Assert
    assertEquals(mockOutputStream, outputStream);
    verify(socket, times(1)).getOutputStream();
  }
}

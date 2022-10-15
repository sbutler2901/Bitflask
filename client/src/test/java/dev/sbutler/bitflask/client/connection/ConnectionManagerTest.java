package dev.sbutler.bitflask.client.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class ConnectionManagerTest {

  @Test
  void close() throws Exception {
    try (MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class)) {
      ConnectionManager connectionManager = new ConnectionManager(new ClientConfiguration());
      Socket mockSocket = socketMockedConstruction.constructed().get(0);
      connectionManager.close();
      verify(mockSocket, times(1)).close();
    }
  }

  @Test
  void getInputStream() throws Exception {
    try (MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class)) {
      ConnectionManager connectionManager = new ConnectionManager(new ClientConfiguration());
      Socket mockSocket = socketMockedConstruction.constructed().get(0);
      InputStream mockInputStream = mock(InputStream.class);
      doReturn(mockInputStream).when(mockSocket).getInputStream();
      InputStream inputStream = connectionManager.getInputStream();
      assertEquals(mockInputStream, inputStream);
      verify(mockSocket, times(1)).getInputStream();
    }
  }

  @Test
  void getOutputStream() throws Exception {
    try (MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class)) {
      ConnectionManager connectionManager = new ConnectionManager(new ClientConfiguration());
      Socket mockSocket = socketMockedConstruction.constructed().get(0);
      OutputStream mockOutputStream = mock(OutputStream.class);
      doReturn(mockOutputStream).when(mockSocket).getOutputStream();
      OutputStream outputStream = connectionManager.getOutputStream();
      assertEquals(mockOutputStream, outputStream);
      verify(mockSocket, times(1)).getOutputStream();
    }
  }
}

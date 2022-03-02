package dev.sbutler.bitflask.server.client_connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.junit.jupiter.api.Test;

public class ClientConnectionModuleTest {

  private final ClientConnectionModule clientConnectionModule = new ClientConnectionModule();

  @Test
  void socket() {
    Socket mockSocket = mock(Socket.class);
    ClientConnectionModule.setSocket(mockSocket);

    Socket socket = clientConnectionModule.provideSocket();

    assertEquals(mockSocket, socket);
  }

  @Test
  void provideClientConnectionManager() {
    ClientConnectionManagerImpl clientConnectionManagerImpl = mock(
        ClientConnectionManagerImpl.class);
    ClientConnectionManager clientConnectionManager = clientConnectionModule.provideClientConnectionManager(
        clientConnectionManagerImpl);
    assertEquals(clientConnectionManagerImpl, clientConnectionManager);
    assertInstanceOf(ClientConnectionManagerImpl.class, clientConnectionManager);
  }

  @Test
  void provideInputStream() throws IOException {
    ClientConnectionManager clientConnectionManager = mock(ClientConnectionManager.class);
    InputStream mockInputStream = mock(InputStream.class);
    doReturn(mockInputStream).when(clientConnectionManager).getInputStream();
    InputStream inputStream = clientConnectionModule.provideInputStream(clientConnectionManager);
    assertEquals(mockInputStream, inputStream);
  }

  @Test
  void provideOutputStream() throws IOException {
    ClientConnectionManager clientConnectionManager = mock(ClientConnectionManager.class);
    OutputStream mockOutputStream = mock(OutputStream.class);
    doReturn(mockOutputStream).when(clientConnectionManager).getOutputStream();
    OutputStream outputStream = clientConnectionModule.provideOutputStream(clientConnectionManager);
    assertEquals(mockOutputStream, outputStream);
  }
}

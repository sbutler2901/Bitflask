package dev.sbutler.bitflask.client.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.jupiter.api.Test;

public class ConnectionModuleTest {

  private final ConnectionModule connectionModule = new ConnectionModule();

  @Test
  void provideServerHost() {
    String serverHost = ConnectionModule.provideServerHost();
    assertEquals("localhost", serverHost);
  }

  @Test
  void provideServerPort() {
    int serverPort = ConnectionModule.provideServerPort();
    assertEquals(9090, serverPort);
  }

  @Test
  void provideInputStream() throws IOException {
    ConnectionManager connectionManager = mock(ConnectionManager.class);
    InputStream mockInputStream = mock(InputStream.class);
    doReturn(mockInputStream).when(connectionManager).getInputStream();
    InputStream inputStream = connectionModule.provideInputStream(connectionManager);
    assertEquals(mockInputStream, inputStream);
  }

  @Test
  void provideOutputStream() throws IOException {
    ConnectionManager connectionManager = mock(ConnectionManager.class);
    OutputStream mockOutputStream = mock(OutputStream.class);
    doReturn(mockOutputStream).when(connectionManager).getOutputStream();
    OutputStream outputStream = connectionModule.provideOutputStream(connectionManager);
    assertEquals(mockOutputStream, outputStream);
  }

}

package dev.sbutler.bitflask.server.client_handling.connection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientConnectionManagerImplTest {

  @InjectMocks
  ClientConnectionManagerImpl clientConnectionManager;

  @Mock
  SocketChannel socketChannel;

  @Test
  void close() throws IOException {
    clientConnectionManager.close();
    verify(socketChannel, times(1)).close();
  }

  @Test
  void getInputStream() throws IOException {
    Socket socket = mock(Socket.class);
    doReturn(socket).when(socketChannel).socket();
    InputStream mockInputStream = mock(InputStream.class);
    doReturn(mockInputStream).when(socket).getInputStream();
    InputStream inputStream = clientConnectionManager.getInputStream();
    assertEquals(mockInputStream, inputStream);
    verify(socketChannel, times(1)).socket();
    verify(socket, times(1)).getInputStream();
  }

  @Test
  void getOutputStream() throws IOException {
    Socket socket = mock(Socket.class);
    doReturn(socket).when(socketChannel).socket();
    OutputStream mockOutputStream = mock(OutputStream.class);
    doReturn(mockOutputStream).when(socket).getOutputStream();
    OutputStream outputStream = clientConnectionManager.getOutputStream();
    assertEquals(mockOutputStream, outputStream);
    verify(socketChannel, times(1)).socket();
    verify(socket, times(1)).getOutputStream();
  }
}

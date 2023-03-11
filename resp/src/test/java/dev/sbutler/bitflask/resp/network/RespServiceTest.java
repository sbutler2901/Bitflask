package dev.sbutler.bitflask.resp.network;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.resp.types.RespElement;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class RespServiceTest {

  private RespService respService;
  private SocketChannel socketChannel;
  private RespReader respReader;
  private RespWriter respWriter;

  @BeforeEach
  void beforeEach() throws Exception {
    socketChannel = mock(SocketChannel.class);
    Socket socket = mock(Socket.class);
    when(socketChannel.socket()).thenReturn(socket);
    when(socket.getInputStream()).thenReturn(mock(InputStream.class));
    when(socket.getOutputStream()).thenReturn(mock(OutputStream.class));

    try (MockedConstruction<RespReader> readerMockedConstruction = mockConstruction(
        RespReader.class);
        MockedConstruction<RespWriter> writerMockedConstruction = mockConstruction(
            RespWriter.class)) {
      respService = RespService.create(socketChannel);
      respReader = readerMockedConstruction.constructed().get(0);
      respWriter = writerMockedConstruction.constructed().get(0);
    }
  }

  @Test
  void read() throws Exception {
    RespElement respElement = mock(RespElement.class);
    when(respReader.readNextRespElement()).thenReturn(respElement);
    // Act
    RespElement element = respService.read();
    // Assert
    assertThat(element).isEqualTo(respElement);
  }

  @Test
  void write() throws Exception {
    // Arrange
    RespElement respElement = mock(RespElement.class);
    // Act
    respService.write(respElement);
    // Assert
    verify(respWriter, times(1)).writeRespElement(eq(respElement));
  }

  @Test
  void close() throws Exception {
    // Act
    respService.close();
    // Assert
    verify(socketChannel, times(1)).close();
  }
}

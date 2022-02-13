package dev.sbutler.bitflask.server.client_processing;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import dev.sbutler.bitflask.server.storage.Storage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientRequestHandlerTest {

  @Mock
  Socket socket;
  @Mock
  Storage storage;

  @Test
  void start_IOException() throws IOException {
    doReturn(new ByteArrayInputStream(new byte[0])).when(socket).getInputStream();
    doReturn(new ByteArrayOutputStream()).when(socket).getOutputStream();
    ClientRequestHandler clientRequestHandler = new ClientRequestHandler(socket, storage);
    clientRequestHandler.run();
    verify(socket, times(1)).close();
  }

  @Test
  void close_IOException() throws IOException {
    doReturn(new ByteArrayInputStream(new byte[0])).when(socket).getInputStream();
    doReturn(new ByteArrayOutputStream()).when(socket).getOutputStream();
    doThrow(new IOException("Test: failure to close socket")).when(socket).close();

    try {
      ClientRequestHandler clientRequestHandler = new ClientRequestHandler(socket, storage);
      clientRequestHandler.run();
    } catch (Exception e) {
      fail();
    }

    verify(socket, times(1)).close();
  }
}

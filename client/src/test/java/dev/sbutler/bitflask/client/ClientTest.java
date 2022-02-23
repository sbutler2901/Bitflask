package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.client.connection.ConnectionManager;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientTest {

  @InjectMocks
  Client client;
  @Mock
  ConnectionManager connectionManager;
  @Mock
  ClientProcessor clientProcessor;

//  @Test
//  void main_start_Success() {
//    MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class,
//        (socketMock, context) -> {
//          doReturn(mock(InputStream.class)).when(socketMock).getInputStream();
//          doReturn(mock(OutputStream.class)).when(socketMock).getOutputStream();
//        });
//    MockedConstruction<RespCommandProcessor> commandProcessorMockedConstruction = mockConstruction(
//        RespCommandProcessor.class);
//    MockedConstruction<Client> clientMockedConstruction = mockConstruction(Client.class);
//
//    Client.main(null);
//    Client client = clientMockedConstruction.constructed().get(0);
//    verify(client, times(1)).start();
//    verify(client, times(1)).close();
//
//    clientMockedConstruction.close();
//    commandProcessorMockedConstruction.close();
//    socketMockedConstruction.close();
//  }
//
//  @Test
//  void main_start_IOException() {
//    try (MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class,
//        (mock, context) -> doThrow(new IOException("Test: socket")).when(mock).getInputStream())) {
//      assertThrows(InternalException.class, () -> Client.main(null));
//    }
//  }

  @Test
  void client_start() {
    client.start();
    verify(clientProcessor, times(1)).start();
  }

  @Test
  void client_close() throws IOException {
    client.close();
    verify(clientProcessor, times(1)).halt();
    verify(connectionManager, times(1)).close();
  }

  @Test
  void client_close_IOException() throws IOException {
    doThrow(new IOException("Test: socket")).when(connectionManager).close();
    assertThrows(InternalException.class, client::close);
    verify(clientProcessor, times(1)).halt();
    verify(connectionManager, times(1)).close();
  }

}

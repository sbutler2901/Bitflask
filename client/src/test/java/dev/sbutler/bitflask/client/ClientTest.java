package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.client.command_processing.CommandProcessor;
import dev.sbutler.bitflask.client.repl.Repl;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientTest {

  @Mock
  Socket socket;
  @Mock
  CommandProcessor commandProcessor;

  @Test
  void main_start_Success() {
    MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class,
        (socketMock, context) -> {
          doReturn(mock(InputStream.class)).when(socketMock).getInputStream();
          doReturn(mock(OutputStream.class)).when(socketMock).getOutputStream();
        });
    MockedConstruction<CommandProcessor> commandProcessorMockedConstruction = mockConstruction(
        CommandProcessor.class);
    MockedConstruction<Client> clientMockedConstruction = mockConstruction(Client.class);

    Client.main(null);
    Client client = clientMockedConstruction.constructed().get(0);
    verify(client, times(1)).start();
    verify(client, times(1)).close();

    clientMockedConstruction.close();
    commandProcessorMockedConstruction.close();
    socketMockedConstruction.close();
  }

  @Test
  void main_start_IOException() {
    try (MockedConstruction<Socket> socketMockedConstruction = mockConstruction(Socket.class,
        (mock, context) -> doThrow(new IOException("Test: socket")).when(mock).getInputStream())) {
      assertThrows(InternalException.class, () -> Client.main(null));
    }
  }

  @Test
  void client_start() {
    try (MockedConstruction<Repl> mockedReplConstruction = mockConstruction(Repl.class)) {
      Client client = new Client(socket, commandProcessor);
      client.start();
      Repl constructedRepl = mockedReplConstruction.constructed().get(0);
      verify(constructedRepl, times(1)).start();
    }
  }

  @Test
  void client_close() throws IOException {
    Client client = new Client(socket, commandProcessor);
    client.close();
    verify(socket, times(1)).close();
  }

  @Test
  void client_close_IOException() throws IOException {
    Client client = new Client(socket, commandProcessor);
    doThrow(new IOException("Test: socket")).when(socket).close();
    assertThrows(InternalException.class, client::close);
    verify(socket, times(1)).close();
  }

}

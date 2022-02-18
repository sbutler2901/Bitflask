package dev.sbutler.bitflask.server;

import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import dev.sbutler.bitflask.server.storage.Storage;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerTest {

  @InjectMocks
  Server server;

  @Mock
  ThreadPoolExecutor threadPoolExecutor;
  @Mock
  Storage storage;
  @Mock
  ServerSocket serverSocket;

  @Test
  void main_success() {
    MockedStatic<Executors> mockedExecutors = mockStatic(Executors.class);
    mockedExecutors.when(() -> Executors.newFixedThreadPool(anyInt()))
        .thenReturn(threadPoolExecutor);
    MockedConstruction<Storage> storageMockedConstruction = mockConstruction(Storage.class);
    MockedConstruction<ServerSocket> socketMockedConstruction = mockConstruction(
        ServerSocket.class);
    MockedConstruction<Server> serverMockedConstruction = mockConstruction(Server.class);

    Server.main(null);

    Server mockedServer = serverMockedConstruction.constructed().get(0);
    verify(mockedServer, times(1)).start();
    verify(mockedServer, times(1)).close();

    serverMockedConstruction.close();
    socketMockedConstruction.close();
    storageMockedConstruction.close();
    mockedExecutors.close();
  }

  @Test
  void server_start() throws IOException {
    MockedConstruction<ClientRequestHandler> clientRequestHandlerMockedConstruction = mockConstruction(
        ClientRequestHandler.class);
    Socket mockClientSocket = mock(Socket.class);
    doReturn(mock(InetAddress.class)).when(mockClientSocket).getInetAddress();
    doReturn(9001).when(mockClientSocket).getPort();
    when(serverSocket.accept()).thenReturn(mockClientSocket)
        .thenThrow(new IOException("test: loop termination"));

    try {
      server.start();
    } catch (InternalException ignored) {
      // ignored, purposefully terminate loop
    }

    ClientRequestHandler mockedClientRequestHandler = clientRequestHandlerMockedConstruction.constructed()
        .get(0);
    verify(threadPoolExecutor, times(1)).execute(mockedClientRequestHandler);

    clientRequestHandlerMockedConstruction.close();
  }

  @Test
  void server_start_IOException() throws IOException {
    doThrow(new IOException("Test: socker accept")).when(serverSocket).accept();
    assertThrows(InternalException.class, () -> server.start());
  }

  @Test
  void server_close() throws IOException {
    server.close();
    verify(threadPoolExecutor, times(1)).shutdown();
    verify(serverSocket, times(1)).close();
  }

  @Test
  void server_close_IOException() throws IOException {
    doThrow(new IOException("Test: socket close")).when(serverSocket).close();
    assertThrows(InternalException.class, () -> server.close());
    verify(threadPoolExecutor, times(1)).shutdown();
  }
}
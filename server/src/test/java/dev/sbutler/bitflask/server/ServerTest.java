package dev.sbutler.bitflask.server;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerTest {

  @InjectMocks
  Server server;

  @Mock
  ExecutorService executorService;
  @Mock
  ServerSocket serverSocket;

  @Test
  void main_success() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector mockedInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(mockedInjector);
      Server mockedServer = mock(Server.class);
      doReturn(mockedServer).when(mockedInjector).getInstance(Server.class);

      Server.main(null);

      verify(mockedServer, times(1)).start(mockedInjector);
      verify(mockedServer, times(1)).close();
    }
  }

  @Test
  void server_start() throws IOException {
    Socket mockClientSocket = mock(Socket.class);
    when(serverSocket.accept()).thenReturn(mockClientSocket)
        .thenThrow(new IOException("test: loop termination"));

    Injector parentInjector = mock(Injector.class);
    Injector childInjector = mock(Injector.class);
    doReturn(childInjector).when(parentInjector).createChildInjector((Module) any());

    ClientRequestHandler mockedClientRequestHandler = mock(ClientRequestHandler.class);
    doReturn(mockedClientRequestHandler).when(childInjector)
        .getInstance(ClientRequestHandler.class);

    try {
      server.start(parentInjector);
    } catch (InternalException ignored) {
      // ignored, purposefully terminate loop
    }

    verify(executorService, times(1)).execute(mockedClientRequestHandler);
  }

  @Test
  void server_start_IOException() throws IOException {
    doThrow(new IOException("Test: socker accept")).when(serverSocket).accept();
    assertThrows(InternalException.class, () -> server.start(mock(Injector.class)));
  }

  @Test
  void server_close() throws IOException {
    server.close();
    verify(executorService, times(1)).shutdown();
    verify(serverSocket, times(1)).close();
  }

  @Test
  void server_close_IOException() throws IOException {
    doThrow(new IOException("Test: socket close")).when(serverSocket).close();
    assertThrows(InternalException.class, () -> server.close());
    verify(executorService, times(1)).shutdown();
  }
}
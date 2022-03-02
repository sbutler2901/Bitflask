package dev.sbutler.bitflask.server;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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
  ServerSocket serverSocket;

  @Test
  void main_success() {
    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector mockedInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(mockedInjector);
      Server mockedServer = mock(Server.class);
      doReturn(mockedServer).when(mockedInjector).getInstance(Server.class);

      Server.main(null);

      verify(mockedServer, times(1)).start();
      verify(mockedServer, times(1)).close();
    }
  }

  @Test
  void server_start() throws IOException {
    Socket mockClientSocket = mock(Socket.class);
    when(serverSocket.accept()).thenReturn(mockClientSocket)
        .thenThrow(new IOException("test: loop termination"));

    try (MockedStatic<Guice> guiceMockedStatic = mockStatic(Guice.class)) {
      Injector mockedInjector = mock(Injector.class);
      guiceMockedStatic.when(() -> Guice.createInjector((Module) any())).thenReturn(mockedInjector);

      ClientRequestHandler mockedClientRequestHandler = mock(ClientRequestHandler.class);
      doReturn(mockedClientRequestHandler).when(mockedInjector)
          .getInstance(ClientRequestHandler.class);

      try {
        server.start();
      } catch (InternalException ignored) {
        // ignored, purposefully terminate loop
      }

      verify(threadPoolExecutor, times(1)).execute(mockedClientRequestHandler);
    }
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
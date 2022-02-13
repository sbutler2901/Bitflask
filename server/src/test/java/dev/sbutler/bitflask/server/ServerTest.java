package dev.sbutler.bitflask.server;

import dev.sbutler.bitflask.server.client_processing.ClientRequestHandler;
import dev.sbutler.bitflask.server.storage.Storage;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

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
  void start_success() throws IOException {
    try (MockedConstruction<ClientRequestHandler> mocked = mockConstruction(
        ClientRequestHandler.class)) {
      Socket clientSocket = mock(Socket.class);
      doReturn(clientSocket).when(serverSocket).accept();
      // easy way to terminate loop
      doThrow(RejectedExecutionException.class).when(threadPoolExecutor)
          .execute(any(Runnable.class));

      server.start();

      verify(serverSocket, times(1)).accept();
      verify(threadPoolExecutor, times(1)).execute(any(Runnable.class));
      verify(threadPoolExecutor, times(1)).shutdown();
    }
  }

  @Test
  void start_IOException_ServerSocket() throws IOException {
    doThrow(new IOException("Test: ServerSocket accept failure")).when(serverSocket).accept();

    server.start();

    verify(serverSocket, times(1)).accept();
    verify(threadPoolExecutor, times(0)).execute(any(Runnable.class));
    verify(threadPoolExecutor, times(1)).shutdown();
  }
}
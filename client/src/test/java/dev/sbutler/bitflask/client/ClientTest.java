package dev.sbutler.bitflask.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sun.jdi.InternalException;
import dev.sbutler.bitflask.client.client_processing.ClientProcessorService;
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
  ClientProcessorService clientProcessorService;

  @Test
  void client_start() {
    client.start();
    verify(clientProcessorService, times(1)).start();
  }

  @Test
  void client_close() throws IOException {
    client.close();
    verify(clientProcessorService, times(1)).halt();
    verify(connectionManager, times(1)).close();
  }

  @Test
  void client_close_IOException() throws IOException {
    doThrow(new IOException("Test: socket")).when(connectionManager).close();
    assertThrows(InternalException.class, client::close);
    verify(clientProcessorService, times(1)).halt();
    verify(connectionManager, times(1)).close();
  }

}

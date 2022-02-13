package dev.sbutler.bitflask.server;

import dev.sbutler.bitflask.server.processing.CommandProcessor;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RequestProcessorTest {

  @InjectMocks
  RequestProcessor requestProcessor;

  @Mock
  CommandProcessor commandProcessor;
  @Mock
  BufferedReader bufferedReader;
  @Mock
  BufferedOutputStream bufferedOutputStream;

}
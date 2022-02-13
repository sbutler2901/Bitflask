package dev.sbutler.bitflask.server;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sbutler.bitflask.resp.RespArray;
import dev.sbutler.bitflask.resp.RespBulkString;
import dev.sbutler.bitflask.resp.RespInteger;
import dev.sbutler.bitflask.resp.RespType;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RequestHandlerTest {

//  private final Socket socket = mock(Socket.class);
//  private final Storage storage = mock(Storage.class);
//  private ByteArrayInputStream byteArrayInputStream;
//  private ByteArrayOutputStream byteArrayOutputStream;
//
//  void prepareSocketMocks(byte[] inputStreamBytes) throws IOException {
//    byteArrayOutputStream = new ByteArrayOutputStream();
//    when(socket.getOutputStream()).thenReturn(byteArrayOutputStream);
//    byteArrayInputStream = new ByteArrayInputStream(inputStreamBytes);
//    when(socket.getInputStream()).thenReturn(byteArrayInputStream);
//  }
//
//  @Test
//  void processRequest_success() throws IOException {
//    // one second read an EOFException will be thrown since the input stream was exhausted
//    // This is fine and allows the call to return
//    RespType<?> clientMessage = new RespArray(List.of(
//        new RespBulkString("ping")
//    ));
//    RespType<?> expectedResponse = new RespBulkString("pong");
//    prepareSocketMocks(clientMessage.getEncodedBytes());
//
//    RequestHandler requestHandler = new RequestHandler(socket, storage);
//    requestHandler.run();
//
//    byte[] response = byteArrayOutputStream.toByteArray();
//    assertArrayEquals(expectedResponse.getEncodedBytes(), response);
//  }
//
//  @Test
//  void processRequest_EOFException() throws IOException {
//    prepareSocketMocks(new byte[0]);
//
//    RequestHandler requestHandler = new RequestHandler(socket, storage);
//    byteArrayInputStream.close();
//    requestHandler.run();
//    verify(socket, times(1)).close();
//  }
//
//  @Test
//  void processRequest_IOException() throws IOException {
//    RespType<?> clientMessage = new RespArray(List.of(
//        new RespBulkString("ping")
//    ));
//    prepareSocketMocks(clientMessage.getEncodedBytes());
//
//    RequestHandler requestHandler = new RequestHandler(socket, storage);
//    requestHandler.run();
//    verify(socket, times(1)).close();
//  }
//
//  @Test
//  void getServerResponseToClient_IllegalArgumentException() throws IOException {
//    RespType<?> invalidClientMessage = new RespInteger(1);
//    prepareSocketMocks(invalidClientMessage.getEncodedBytes());
//
//    RequestHandler requestHandler = new RequestHandler(socket, storage);
//    requestHandler.run();
//
//    String response = byteArrayOutputStream.toString();
//    assertTrue(response.contains("Invalid"));
//  }

}

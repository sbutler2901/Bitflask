package bitflask.resp;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Resp {

  private final BufferedOutputStream bufferedOutputStream;
  private final BufferedReader bufferedReader;

  public Resp(Socket socket) throws IOException {
    this.bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
    this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
  }

  public void send(RespType respType) throws IOException {
    respType.write(bufferedOutputStream);
    bufferedOutputStream.flush();
  }

  private static RespType parseResptype(BufferedReader bufferedReader) throws IOException {
    int code = bufferedReader.read();
    if (code == -1) {
      return null;
    }
    switch (code) {
      case RespSimpleString.TYPE_PREFIX:
        return new RespSimpleString(bufferedReader);
      case RespBulkString.TYPE_PREFIX:
        return new RespBulkString(bufferedReader);
      case RespInteger.TYPE_PREFIX:
        return new RespInteger(bufferedReader);
      case RespError.TYPE_PREFIX:
        return new RespError(bufferedReader);
      case RespArray.TYPE_PREFIX:
        return new RespArray(bufferedReader);
      default:
        return null;
    }
  }

  public RespType receive() throws IOException {
    return parseResptype(bufferedReader);
  }

  public static RespType receive(BufferedReader bufferedReader) throws IOException {
    return parseResptype(bufferedReader);
  }
}

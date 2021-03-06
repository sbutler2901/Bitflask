package dev.sbutler.bitflask.client.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ConnectionManager {

  void close() throws IOException;

  InputStream getInputStream() throws IOException;

  OutputStream getOutputStream() throws IOException;
}

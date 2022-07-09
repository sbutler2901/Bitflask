package dev.sbutler.bitflask.server.client_handling_service.connection;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ClientConnectionManager extends Closeable {

  void close() throws IOException;

  InputStream getInputStream() throws IOException;

  OutputStream getOutputStream() throws IOException;
}

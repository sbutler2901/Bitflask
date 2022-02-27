package dev.sbutler.bitflask.server.client_processing;

import dev.sbutler.bitflask.resp.network.reader.RespReaderImpl;
import dev.sbutler.bitflask.resp.network.writer.RespWriterImpl;
import dev.sbutler.bitflask.server.command_processing.CommandProcessor;
import dev.sbutler.bitflask.server.storage.Storage;
import java.io.IOException;
import java.net.Socket;

public class ClientRequestHandler implements Runnable {

  private static final String TERMINATING_CONNECTION = "Terminating session.";

  private final Socket socket;
  private final ClientMessageProcessor clientMessageProcessor;
  private boolean shouldContinueRunning = true;

  public ClientRequestHandler(Socket socket, Storage storage) throws IOException {
    this.socket = socket;
    this.clientMessageProcessor = new ClientMessageProcessor(
        new CommandProcessor(storage),
        new RespReaderImpl(socket.getInputStream()),
        new RespWriterImpl(socket.getOutputStream())
    );
  }

  @Override
  public void run() {
    while (!Thread.interrupted() && shouldContinueRunning) {
      shouldContinueRunning = clientMessageProcessor.processNextMessage();
    }
    this.close();
  }

  public void close() {
    try {
      shouldContinueRunning = false;
      socket.close();
      System.out.println(TERMINATING_CONNECTION);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

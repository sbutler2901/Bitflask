package dev.sbutler.bitflask.client.client_processing;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplIOException;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplParser;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplReader;
import dev.sbutler.bitflask.client.client_processing.input.repl.ReplSyntaxException;
import dev.sbutler.bitflask.client.client_processing.input.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Provides a REPL shell for a client to interactively execute commands.
 */
public class ReplClientProcessorService implements ClientProcessorService {

  private static final String SHELL_PREFIX = "> ";

  public static class Factory {

    private final ClientProcessor clientProcessor;
    private final OutputWriter outputWriter;

    @Inject
    public Factory(ClientProcessor clientProcessor, OutputWriter outputWriter) {
      this.clientProcessor = clientProcessor;
      this.outputWriter = outputWriter;
    }

    public ReplClientProcessorService create(ReplReader replReader) {
      return new ReplClientProcessorService(clientProcessor, replReader, outputWriter);
    }
  }

  private final ClientProcessor clientProcessor;
  private final ReplReader replReader;
  private final OutputWriter outputWriter;

  private boolean continueProcessingClientInput = true;

  private ReplClientProcessorService(ClientProcessor clientProcessor,
      ReplReader replReader,
      OutputWriter outputWriter) {
    this.clientProcessor = clientProcessor;
    this.replReader = replReader;
    this.outputWriter = outputWriter;
  }

  @Override
  public void run() {
    // Doesn't work when running from IDE (https://youtrack.jetbrains.com/issue/IDEA-18814)
    // boolean hasConsole = System.console() != null;
    while (continueProcessingClientInput) {
      outputWriter.write(SHELL_PREFIX);
      try {
        ImmutableList<ReplElement> clientInput = ReplParser.readNextLine(replReader);
        if (clientInput == null) {
          triggerShutdown();
          return;
        }
        if (!clientInput.isEmpty()) {
          processClientInput(clientInput);
        }
      } catch (ReplSyntaxException e) {
        // TODO: handle cleanup of repl reader
        outputWriter.writeWithNewLine(e.getMessage());
      } catch (ReplIOException e) {
        outputWriter.writeWithNewLine(e.getMessage());
        triggerShutdown();
      }
    }
  }

  private void processClientInput(ImmutableList<ReplElement> clientInput) {
    try {
      boolean shouldContinueProcessing = clientProcessor.processClientInput(clientInput);
      if (!shouldContinueProcessing) {
        triggerShutdown();
      }
    } catch (ClientProcessingException e) {
      // TODO: handle cleanup of repl reader
      outputWriter.writeWithNewLine(e.getMessage());
    }
  }

  public void triggerShutdown() {
    continueProcessingClientInput = false;
    try {
      replReader.close();
    } catch (IOException e) {
      // ignored
    }
  }
}

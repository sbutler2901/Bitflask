package dev.sbutler.bitflask.client.client_processing;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.repl.ReplIOException;
import dev.sbutler.bitflask.client.client_processing.repl.ReplParser;
import dev.sbutler.bitflask.client.client_processing.repl.ReplReader;
import dev.sbutler.bitflask.client.client_processing.repl.ReplSyntaxException;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import java.io.IOException;
import java.util.Optional;
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

    public ReplClientProcessorService create(ReplReader replReader, boolean usePrompt) {
      return new ReplClientProcessorService(clientProcessor, replReader, outputWriter, usePrompt);
    }
  }

  private final ClientProcessor clientProcessor;
  private final ReplReader replReader;
  private final OutputWriter outputWriter;
  private final boolean usePrompt;

  private boolean continueProcessingClientInput = true;
  private boolean shouldCleanup = false;

  private ReplClientProcessorService(ClientProcessor clientProcessor,
      ReplReader replReader,
      OutputWriter outputWriter,
      boolean usePrompt) {
    this.clientProcessor = clientProcessor;
    this.replReader = replReader;
    this.outputWriter = outputWriter;
    this.usePrompt = usePrompt;
  }

  @Override
  public void run() {
    // Doesn't work when running from IDE (https://youtrack.jetbrains.com/issue/IDEA-18814)
    // boolean hasConsole = System.console() != null;
    while (continueProcessingClientInput) {
      if (shouldCleanup) {
        try {
          ReplParser.cleanupForNextLine(replReader);
          shouldCleanup = false;
          continue;
        } catch (ReplIOException e) {
          outputWriter.writeWithNewLine(e.getMessage());
          triggerShutdown();
          break;
        }
      }

      if (usePrompt) {
        outputWriter.write(SHELL_PREFIX);
      }
      getAndProcessClientInput();
    }
  }

  private void getAndProcessClientInput() {
    try {
      Optional<ImmutableList<ReplElement>> clientInputOptional =
          ReplParser.readNextLine(replReader);
      clientInputOptional.ifPresentOrElse(this::processClientInput, this::triggerShutdown);
    } catch (ReplSyntaxException e) {
      shouldCleanup = true;
      outputWriter.writeWithNewLine(e.getMessage());
    } catch (ReplIOException e) {
      outputWriter.writeWithNewLine(e.getMessage());
      triggerShutdown();
    }
  }

  private void processClientInput(ImmutableList<ReplElement> clientInput) {
    boolean shouldContinueProcessing = clientProcessor.processClientInput(clientInput);
    if (!shouldContinueProcessing) {
      triggerShutdown();
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

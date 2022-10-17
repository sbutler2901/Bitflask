package dev.sbutler.bitflask.client.client_processing;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import java.io.IOException;
import java.text.ParseException;
import javax.inject.Inject;

/**
 * Provides a REPL shell for a client to interactively execute commands.
 */
public class ReplClientProcessorService implements Runnable {

  private static final String SHELL_PREFIX = "> ";

  private final ClientProcessor clientProcessor;
  private final InputParser inputParser;
  private final OutputWriter outputWriter;

  private boolean continueProcessingClientInput = true;

  @Inject
  public ReplClientProcessorService(ClientProcessor clientProcessor,
      InputParser inputParser,
      OutputWriter outputWriter) {
    this.clientProcessor = clientProcessor;
    this.inputParser = inputParser;
    this.outputWriter = outputWriter;
  }

  @Override
  public void run() {
    // Doesn't work when running from IDE (https://youtrack.jetbrains.com/issue/IDEA-18814)
    // boolean hasConsole = System.console() != null;
    while (continueProcessingClientInput) {
      outputWriter.write(SHELL_PREFIX);
      ImmutableList<String> clientInput;
      try {
        clientInput = inputParser.getClientNextInput();
        if (clientInput == null) {
          triggerShutdown();
          break;
        }
        boolean shouldContinueProcessing = clientProcessor.processClientInput(clientInput);
        if (!shouldContinueProcessing) {
          triggerShutdown();
        }
      } catch (ParseException e) {
        outputWriter.writeWithNewLine(e.getMessage());
      } catch (IOException e) {
        outputWriter.writeWithNewLine(e.getMessage());
        triggerShutdown();
      }
    }
  }

  public void triggerShutdown() {
    continueProcessingClientInput = false;
  }
}

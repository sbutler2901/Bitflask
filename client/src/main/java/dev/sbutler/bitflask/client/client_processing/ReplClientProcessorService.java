package dev.sbutler.bitflask.client.client_processing;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import dev.sbutler.bitflask.client.client_processing.input.InputParser;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import javax.inject.Inject;

public class ReplClientProcessorService extends AbstractExecutionThreadService {

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
  protected void run() {
    while (continueProcessingClientInput) {
      outputWriter.write(SHELL_PREFIX);
      ImmutableList<String> clientInput = inputParser.getClientNextInput();
      boolean shouldContinueProcessing = clientProcessor.processClientInput(clientInput);
      if (!shouldContinueProcessing) {
        stopProcessingClientInput();
      }
    }
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  protected void triggerShutdown() {
    stopProcessingClientInput();
  }

  private void stopProcessingClientInput() {
    continueProcessingClientInput = false;
  }
}
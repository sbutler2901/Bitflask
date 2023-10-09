package dev.sbutler.bitflask.client;

import com.google.common.base.Joiner;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.client.client_processing.output.OutputWriter;
import dev.sbutler.bitflask.client.client_processing.output.StdoutOutputWriter;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.resp.network.RespServiceProvider;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

public class ClientModule extends AbstractModule {

  private final InlineCommand inlineCommand;

  public ClientModule(InlineCommand inlineCommand) {
    this.inlineCommand = inlineCommand;
  }

  @Override
  protected void configure() {
    bind(OutputWriter.class).to(StdoutOutputWriter.class);
    bind(RespService.class).toProvider(RespServiceProvider.class);
  }

  @Provides
  ExecutionMode provideExecutionMode() {
    return inlineCommand.isEmpty() ? ExecutionMode.REPL : ExecutionMode.INLINE;
  }

  @Singleton
  @Provides
  Reader provideReader(ExecutionMode executionMode) {
    if (executionMode.isReplMode()) {
      return new BufferedReader(new InputStreamReader(System.in));
    }

    String inlineCmd = Joiner.on(' ').join(inlineCommand.getArgs());
    return new BufferedReader(new StringReader(inlineCmd));
  }
}

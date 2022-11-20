package dev.sbutler.bitflask.client.client_processing;

import com.google.common.base.Joiner;
import dev.sbutler.bitflask.client.configuration.ClientConfigurations;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import javax.inject.Inject;
import javax.inject.Provider;

class ReaderProvider implements Provider<Reader> {

  private final ClientConfigurations configurations;

  @Inject
  ReaderProvider(ClientConfigurations configurations) {
    this.configurations = configurations;
  }

  @Override
  public Reader get() {
    if (configurations.getUsePrompt()) {
      return new InputStreamReader(System.in);
    }

    String inlineCmd = Joiner.on(' ').join(configurations.getInlineCmd());
    return new StringReader(inlineCmd);
  }
}

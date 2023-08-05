package dev.sbutler.bitflask.client;

import com.google.common.collect.ImmutableList;

public final class InlineCommand {
  private final ImmutableList<String> inlineCommandArgs;

  public InlineCommand(String[] inlineCommandArgs) {
    this.inlineCommandArgs = ImmutableList.copyOf(inlineCommandArgs);
  }

  public boolean isEmpty() {
    return inlineCommandArgs.isEmpty();
  }

  public ImmutableList<String> getArgs() {
    return inlineCommandArgs;
  }
}

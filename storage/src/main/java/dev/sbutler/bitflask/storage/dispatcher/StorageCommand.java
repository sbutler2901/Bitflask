package dev.sbutler.bitflask.storage.dispatcher;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;

public record StorageCommand(Type type, ImmutableList<String> arguments) {

  public enum Type {
    READ,
    WRITE
  }

  public StorageCommand {
    checkNotNull(type);
    checkNotNull(arguments);
    switch (type) {
      case READ -> validateReadArgs(arguments);
      case WRITE -> validateWriteArgs(arguments);
    }
  }

  private static void validateReadArgs(ImmutableList<String> args) {
    checkArgument(args.size() == 1,
        "A read should have one argument, the key");
    String key = args.get(0);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
  }

  private static void validateWriteArgs(ImmutableList<String> args) {
    checkArgument(args.size() == 2,
        "A write should have two arguments, the key and value");
    String key = args.get(0);
    String value = args.get(1);
    checkArgument(!key.isBlank(), "Expected non-blank key, but was [%s]", key);
    checkArgument(key.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        key.length());
    checkArgument(!value.isBlank(), "Expected non-blank key, but was [%s]", value);
    checkArgument(value.length() <= 256, "Expect key smaller than 256 characters, but was [%d]",
        value.length());
  }
}

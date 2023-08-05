package dev.sbutler.bitflask.config;

import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility for loading {@link BitflaskConfig} from disk. */
public final class ConfigLoader {

  /** Loads a {@link BitflaskConfig} from {@link ConfigDefaults#CONFIG_OUTPUT_FILE}. */
  public static BitflaskConfig load() throws IOException {
    return loadFromPath(ConfigDefaults.CONFIG_OUTPUT_FILE);
  }

  /** Loads a {@link BitflaskConfig} from the provided path. */
  public static BitflaskConfig loadFromPath(Path configOutputFile) throws IOException {
    if (!Files.exists(configOutputFile)) {
      throw new IllegalArgumentException(configOutputFile.toAbsolutePath() + " does not exist.");
    }

    String configJson = Files.readString(configOutputFile, StandardCharsets.UTF_8);
    BitflaskConfig.Builder configBuilder = BitflaskConfig.newBuilder();
    JsonFormat.parser().merge(configJson, configBuilder);
    return configBuilder.build();
  }

  private ConfigLoader() {}
}

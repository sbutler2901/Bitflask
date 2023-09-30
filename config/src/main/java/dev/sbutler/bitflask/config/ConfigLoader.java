package dev.sbutler.bitflask.config;

import com.google.protobuf.util.JsonFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Utility for loading {@link BitflaskConfig} from disk. */
public final class ConfigLoader {

  /** Loads a {@link BitflaskConfig} from {@link ConfigDefaults#CONFIG_OUTPUT_FILE}. */
  public static BitflaskConfig load(ConfigLoaderOptions options) throws IOException {
    return loadFromPath(options, ConfigDefaults.CONFIG_OUTPUT_FILE);
  }

  /** Loads a {@link BitflaskConfig} from the provided path. */
  public static BitflaskConfig loadFromPath(ConfigLoaderOptions options, Path configOutputFile)
      throws IOException {
    if (!Files.exists(configOutputFile)) {
      throw new IllegalArgumentException(configOutputFile.toAbsolutePath() + " does not exist.");
    }

    BitflaskConfig loadedConfig = loadBitflaskConfig(configOutputFile);
    BitflaskConfig finalConfig = buildFinalBitflaskConfig(options, loadedConfig);
    ConfigValidator.validate(finalConfig);

    return finalConfig;
  }

  private static BitflaskConfig loadBitflaskConfig(Path configOutputFile) throws IOException {
    String configJson = Files.readString(configOutputFile, StandardCharsets.UTF_8);
    BitflaskConfig.Builder configBuilder = BitflaskConfig.newBuilder();
    JsonFormat.parser().ignoringUnknownFields().merge(configJson, configBuilder);
    return configBuilder.build();
  }

  private static BitflaskConfig buildFinalBitflaskConfig(
      ConfigLoaderOptions options, BitflaskConfig loadedConfig) {
    BitflaskConfig.Builder builder = loadedConfig.toBuilder();
    builder.setServerConfig(buildFinalServerConfig(options, builder.getServerConfig()));
    return builder.build();
  }

  private static ServerConfig buildFinalServerConfig(
      ConfigLoaderOptions options, ServerConfig serverConfig) {
    ServerConfig.Builder builder = serverConfig.toBuilder();
    builder.setThisServerId(options.thisServerId());
    return builder.build();
  }

  /** Options for configuring the {@link BitflaskConfig} loaded by {@link ConfigLoader}. */
  public record ConfigLoaderOptions(String thisServerId) {}

  private ConfigLoader() {}
}

package dev.sbutler.bitflask.config;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.util.JsonFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/** CLI tool for building a default {@link BitflaskConfig} as a json file. */
public final class ConfigWriter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  public static void main(String[] args) throws Exception {
    BitflaskConfig bitflaskConfig = ConfigDefaults.BITFLASK_CONFIG;
    ConfigValidator.validateForWrite(bitflaskConfig);

    String configJson = JsonFormat.printer().print(bitflaskConfig);

    Files.createDirectories(ConfigDefaults.CONFIG_OUTPUT_DIR);
    Files.writeString(ConfigDefaults.CONFIG_OUTPUT_FILE, configJson, StandardCharsets.UTF_8);

    logger.atInfo().log(
        "Config writen to [%s]", ConfigDefaults.CONFIG_OUTPUT_FILE.toAbsolutePath().toString());
    logger.atInfo().log(configJson);
  }

  private ConfigWriter() {}
}

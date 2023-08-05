package dev.sbutler.bitflask.config;

import com.google.common.flogger.FluentLogger;
import com.google.protobuf.util.JsonFormat;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** CLI tool for building BitflaskConfig as a json file. */
public final class ConfigWriter {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String configFileName = "bitflask_config.json";

  public static void main(String[] args) throws Exception {
    BitflaskConfig bitflaskConfig = ConfigDefaults.BITFLASK_CONFIG;
    ConfigValidator.validate(bitflaskConfig);

    String configJson = JsonFormat.printer().print(bitflaskConfig);

    Path configOutputDir = Paths.get(System.getProperty("user.home") + "/.bitflask/config");
    Path configOutputFile = configOutputDir.resolve(configFileName);

    Files.createDirectories(configOutputDir);
    Files.writeString(configOutputFile, configJson, StandardCharsets.UTF_8);

    logger.atInfo().log("Config writen to [%s]", configOutputFile.toAbsolutePath().toString());
    logger.atInfo().log(configJson);
  }

  private ConfigWriter() {}
}

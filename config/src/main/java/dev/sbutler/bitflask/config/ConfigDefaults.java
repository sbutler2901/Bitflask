package dev.sbutler.bitflask.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class ConfigDefaults {

  static final Path CONFIG_OUTPUT_DIR =
      Paths.get(System.getProperty("user.home") + "/.bitflask/config");
  static final String CONFIG_FILE_NAME = "bitflask_config.json";
  static final Path CONFIG_OUTPUT_FILE = CONFIG_OUTPUT_DIR.resolve(CONFIG_FILE_NAME);

  private ConfigDefaults() {}
}

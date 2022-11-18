package dev.sbutler.bitflask.common.configuration;

import com.google.common.collect.ImmutableMap;

/**
 * A mapping of various command line flag(s) to their corresponding configuration.
 */
public class ConfigurationFlagMap {

  private final ImmutableMap<String, Configuration> map;

  private ConfigurationFlagMap(ImmutableMap<String, Configuration> map) {
    this.map = map;
  }

  /**
   * Gets the {@link Configuration} mapped by this flag, or null if no mapping exists
   */
  public Configuration get(String flag) {
    return map.get(flag);
  }

  public static class Builder {

    private final ImmutableMap.Builder<String, Configuration> map = new ImmutableMap.Builder<>();

    /**
     * Maps the flag to the configuration
     */
    public Builder put(String flag, Configuration configuration) {
      map.put(flag, configuration);
      return this;
    }

    /**
     * Maps all flags to the configuration
     */
    public Builder putAll(Iterable<String> flags, Configuration configuration) {
      flags.forEach(f -> map.put(f, configuration));
      return this;
    }

    /**
     * Builds a new {@link dev.sbutler.bitflask.common.configuration.ConfigurationFlagMap}.
     */
    public ConfigurationFlagMap build() {
      return new ConfigurationFlagMap(map.build());
    }
  }
}

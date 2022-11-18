package dev.sbutler.bitflask.server.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.common.configuration.Configuration;

public class ServerConfigurationsConstants {

  // Server Port
  static final String SERVER_PORT_FLAG_SHORT = "-p";
  static final String SERVER_PORT_FLAG_LONG = "--port";
  static final String SERVER_PORT_PROPERTY_KEY = "server.port";
  static final int DEFAULT_SERVER_PORT = 9090;
  static final Configuration SERVER_PORT_CONFIGURATION = new Configuration(
      ImmutableList.of(SERVER_PORT_FLAG_SHORT, SERVER_PORT_FLAG_LONG),
      SERVER_PORT_PROPERTY_KEY,
      DEFAULT_SERVER_PORT
  );

  public static final ImmutableMap<String, Configuration> SERVER_FLAG_TO_CONFIGURATION_MAP =
      new ImmutableMap.Builder<String, Configuration>()
          .put(SERVER_PORT_FLAG_SHORT, SERVER_PORT_CONFIGURATION)
          .put(SERVER_PORT_FLAG_LONG, SERVER_PORT_CONFIGURATION)
          .build();

  private ServerConfigurationsConstants() {

  }
}

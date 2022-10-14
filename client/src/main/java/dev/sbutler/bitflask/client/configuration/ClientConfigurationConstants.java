package dev.sbutler.bitflask.client.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dev.sbutler.bitflask.common.configuration.Configuration;

public class ClientConfigurationConstants {

  // Server Host
  static final String SERVER_HOST_FLAG_SHORT = "-h";
  static final String SERVER_HOST_FLAG_LONG = "--host";
  static final String SERVER_HOST_PROPERTY_KEY = "server.host";
  static final String DEFAULT_SERVER_HOST = "localhost";
  static final Configuration SERVER_HOST_CONFIGURATION = new Configuration(
      ImmutableList.of(SERVER_HOST_FLAG_SHORT, SERVER_HOST_FLAG_LONG),
      SERVER_HOST_PROPERTY_KEY,
      DEFAULT_SERVER_HOST
  );

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

  public static final ImmutableMap<String, Configuration> CLIENT_FLAG_TO_CONFIGURATION_MAP =
      new ImmutableMap.Builder<String, Configuration>()
          // Server Host
          .put(SERVER_HOST_FLAG_SHORT, SERVER_HOST_CONFIGURATION)
          .put(SERVER_HOST_FLAG_LONG, SERVER_HOST_CONFIGURATION)
          // Server Port
          .put(SERVER_PORT_FLAG_SHORT, SERVER_PORT_CONFIGURATION)
          .put(SERVER_PORT_FLAG_LONG, SERVER_PORT_CONFIGURATION)
          .build();

  private ClientConfigurationConstants() {

  }
}

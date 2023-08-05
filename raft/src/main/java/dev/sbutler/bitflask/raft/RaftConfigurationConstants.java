package dev.sbutler.bitflask.raft;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.common.configuration.Configuration;
import dev.sbutler.bitflask.common.configuration.ConfigurationFlagMap;

public final class RaftConfigurationConstants {
  // This server id
  static final String THIS_SERVER_ID_FLAG = "--raftThisServerId";
  static final String THIS_SERVER_ID_PROPERTY_KEY = "raft.thisServerId";
  static final String THIS_SERVER_ID_DEFAULT = "server_0";
  static final Configuration THIS_SERVER_ID_CONFIGURATION =
      new Configuration(
          ImmutableList.of(THIS_SERVER_ID_FLAG),
          THIS_SERVER_ID_PROPERTY_KEY,
          THIS_SERVER_ID_DEFAULT);

  // Timer Interval
  static final String TIMER_MINIMUM_MS_FLAG = "--raftTimerMinimumMs";
  static final String TIMER_MINIMUM_MS_PROPERTY_KEY = "raft.timerMinimumMs";
  static final int TIMER_MINIMUM_MS_DEFAULT = 150;
  static final Configuration TIMER_MINIMUM_MS_CONFIGURATION =
      new Configuration(
          ImmutableList.of(TIMER_MINIMUM_MS_FLAG),
          TIMER_MINIMUM_MS_PROPERTY_KEY,
          TIMER_MINIMUM_MS_DEFAULT);

  static final String TIMER_MAXIMUM_MS_FLAG = "--raftTimerMaximumMs";
  static final String TIMER_MAXIMUM_MS_PROPERTY_KEY = "raft.timerMaximumMs";
  static final int TIMER_MAXIMUM_MS_DEFAULT = 150;
  static final Configuration TIMER_MAXIMUM_MS_CONFIGURATION =
      new Configuration(
          ImmutableList.of(TIMER_MAXIMUM_MS_FLAG),
          TIMER_MAXIMUM_MS_PROPERTY_KEY,
          TIMER_MAXIMUM_MS_DEFAULT);

  public static final ConfigurationFlagMap RAFT_FLAG_TO_CONFIGURATION_MAP =
      new ConfigurationFlagMap.Builder()
          .put(THIS_SERVER_ID_FLAG, THIS_SERVER_ID_CONFIGURATION)
          .put(TIMER_MINIMUM_MS_FLAG, TIMER_MINIMUM_MS_CONFIGURATION)
          .put(TIMER_MAXIMUM_MS_FLAG, TIMER_MAXIMUM_MS_CONFIGURATION)
          .build();

  private RaftConfigurationConstants() {}
}

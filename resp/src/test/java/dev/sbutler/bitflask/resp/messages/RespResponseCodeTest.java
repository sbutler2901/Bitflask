package dev.sbutler.bitflask.resp.messages;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link RespResponseCode}. */
public class RespResponseCodeTest {

  @Test
  public void fromValue_success() {
    assertThat(RespResponseCode.fromValue(0)).isEqualTo(RespResponseCode.SUCCESS);
  }

  @Test
  public void fromValue_failure() {
    assertThat(RespResponseCode.fromValue(1)).isEqualTo(RespResponseCode.FAILURE);
  }

  @Test
  public void fromValue_notCurrentLeader() {
    assertThat(RespResponseCode.fromValue(2)).isEqualTo(RespResponseCode.NOT_CURRENT_LEADER);
  }

  @Test
  public void fromValue_noKnownLeader() {
    assertThat(RespResponseCode.fromValue(3)).isEqualTo(RespResponseCode.NO_KNOWN_LEADER);
  }
}

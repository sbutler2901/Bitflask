package dev.sbutler.bitflask.resp.messages;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link RespRequestCode}. */
public class RespRequestCodeTest {

  @Test
  public void fromValue_ping() {
    assertThat(RespRequestCode.fromValue(0)).isEqualTo(RespRequestCode.PING);
  }

  @Test
  public void fromValue_get() {
    assertThat(RespRequestCode.fromValue(1)).isEqualTo(RespRequestCode.GET);
  }

  @Test
  public void fromValue_set() {
    assertThat(RespRequestCode.fromValue(2)).isEqualTo(RespRequestCode.SET);
  }

  @Test
  public void fromValue_delete() {
    assertThat(RespRequestCode.fromValue(3)).isEqualTo(RespRequestCode.DELETE);
  }
}

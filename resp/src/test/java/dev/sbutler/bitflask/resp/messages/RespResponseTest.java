package dev.sbutler.bitflask.resp.messages;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespInteger;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RespResponse}. */
public class RespResponseTest {

  @Test
  public void createFromRespArray_success_identity() {
    var response = new RespResponse.Success("message");

    var createdResponse = RespResponse.createFromRespArray(response.getAsRespArray());

    assertThat(createdResponse).isEqualTo(response);
  }

  @Test
  public void createFromRespArray_failure_identity() {
    var response = new RespResponse.Failure("message");

    var createdResponse = RespResponse.createFromRespArray(response.getAsRespArray());

    assertThat(createdResponse).isEqualTo(response);
  }

  @Test
  public void createFromRespArray_notCurrentLeader_identity() {
    var response = new RespResponse.NotCurrentLeader("host", 9090);

    var createdResponse = RespResponse.createFromRespArray(response.getAsRespArray());

    assertThat(createdResponse).isEqualTo(response);
  }

  @Test
  public void createFromRespArray_noKnownLeader_identity() {
    var response = new RespResponse.NoKnownLeader("message");

    var createdResponse = RespResponse.createFromRespArray(response.getAsRespArray());

    assertThat(createdResponse).isEqualTo(response);
  }

  @Test
  public void createFromRespArray_emptyRespArray_throwsRespResponseConversionException() {
    var array = new RespArray(ImmutableList.of());

    RespResponseConversionException exception =
        assertThrows(
            RespResponseConversionException.class, () -> RespResponse.createFromRespArray(array));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(String.format("Failed to convert %s to a RespResponse.", array));
    assertThat(exception).hasCauseThat().isInstanceOf(ArrayIndexOutOfBoundsException.class);
  }

  @Test
  public void success() {
    var response = new RespResponse.Success("message");

    assertThat(response.getResponseCode()).isEqualTo(RespResponseCode.SUCCESS);
    assertThat(response.getMessage()).isEqualTo("message");
    assertThat(response.getAsRespArray().getValue())
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespResponseCode.SUCCESS.getValue()),
                new RespBulkString("message")));
  }

  @Test
  public void failure() {
    var response = new RespResponse.Failure("message");

    assertThat(response.getResponseCode()).isEqualTo(RespResponseCode.FAILURE);
    assertThat(response.getMessage()).isEqualTo("message");
    assertThat(response.getAsRespArray().getValue())
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespResponseCode.FAILURE.getValue()),
                new RespBulkString("message")));
  }

  @Test
  public void notCurrentLeader() {
    var response = new RespResponse.NotCurrentLeader("host", 9090);

    assertThat(response.getResponseCode()).isEqualTo(RespResponseCode.NOT_CURRENT_LEADER);
    assertThat(response.getMessage()).isEqualTo("Current leader: [host=host, respPort=9090].");
    assertThat(response.getAsRespArray().getValue())
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespResponseCode.NOT_CURRENT_LEADER.getValue()),
                new RespBulkString("Current leader: [host=host, respPort=9090]."),
                new RespBulkString("host"),
                new RespInteger(9090)));
  }

  @Test
  public void noKnownLeader() {
    var response = new RespResponse.NoKnownLeader("message");

    assertThat(response.getResponseCode()).isEqualTo(RespResponseCode.NO_KNOWN_LEADER);
    assertThat(response.getMessage()).isEqualTo("message");
    assertThat(response.getAsRespArray().getValue())
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespResponseCode.NO_KNOWN_LEADER.getValue()),
                new RespBulkString("message")));
  }
}

package dev.sbutler.bitflask.resp.messages;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespInteger;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RespRequest}. */
public class RespRequestTest {

  @Test
  public void createFromRespArray_ping_identity() {
    var request = new RespRequest.PingRequest();

    var createdRequest = RespRequest.createFromRespArray(request.getAsRespArray());

    assertThat(createdRequest).isEqualTo(request);
  }

  @Test
  public void createFromRespArray_get_identity() {
    var request = new RespRequest.GetRequest("key");

    var createdRequest = RespRequest.createFromRespArray(request.getAsRespArray());

    assertThat(createdRequest).isEqualTo(request);
  }

  @Test
  public void createFromRespArray_set_identity() {
    var request = new RespRequest.SetRequest("key", "value");

    var createdRequest = RespRequest.createFromRespArray(request.getAsRespArray());

    assertThat(createdRequest).isEqualTo(request);
  }

  @Test
  public void createFromRespArray_delete_identity() {
    var request = new RespRequest.DeleteRequest("key");

    var createdRequest = RespRequest.createFromRespArray(request.getAsRespArray());

    assertThat(createdRequest).isEqualTo(request);
  }

  @Test
  public void createFromRespArray_emptyRespArray_throwsRespRequestConversionException() {
    var array = new RespArray(ImmutableList.of());

    RespRequestConversionException exception =
        assertThrows(
            RespRequestConversionException.class, () -> RespRequest.createFromRespArray(array));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(String.format("Failed to convert %s to a RespRequest.", array));
    assertThat(exception).hasCauseThat().isInstanceOf(ArrayIndexOutOfBoundsException.class);
  }

  @Test
  public void ping() {
    var request = new RespRequest.PingRequest();

    List<RespElement> elements = request.getAsRespArray().getValue();

    assertThat(request.getRequestCode()).isEqualTo(RespRequestCode.PING);
    assertThat(elements)
        .containsExactlyElementsIn(
            ImmutableList.of(new RespInteger(RespRequestCode.PING.getValue())));
  }

  @Test
  public void get() {
    var request = new RespRequest.GetRequest("key");

    List<RespElement> elements = request.getAsRespArray().getValue();

    assertThat(request.getRequestCode()).isEqualTo(RespRequestCode.GET);
    assertThat(request.getKey()).isEqualTo("key");
    assertThat(elements)
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespRequestCode.GET.getValue()), new RespBulkString("key")));
  }

  @Test
  public void set() {
    var request = new RespRequest.SetRequest("key", "value");

    List<RespElement> elements = request.getAsRespArray().getValue();

    assertThat(request.getRequestCode()).isEqualTo(RespRequestCode.SET);
    assertThat(request.getKey()).isEqualTo("key");
    assertThat(request.getValue()).isEqualTo("value");
    assertThat(elements)
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespRequestCode.SET.getValue()),
                new RespBulkString("key"),
                new RespBulkString("value")));
  }

  @Test
  public void delete() {
    var request = new RespRequest.DeleteRequest("key");

    List<RespElement> elements = request.getAsRespArray().getValue();

    assertThat(request.getRequestCode()).isEqualTo(RespRequestCode.DELETE);
    assertThat(request.getKey()).isEqualTo("key");
    assertThat(elements)
        .containsExactlyElementsIn(
            ImmutableList.of(
                new RespInteger(RespRequestCode.DELETE.getValue()), new RespBulkString("key")));
  }
}

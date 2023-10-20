package dev.sbutler.bitflask.resp.messages;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespInteger;
import java.util.List;

/** A request sent to a Bitflask server when using its RESP based API. */
public abstract sealed class RespRequest
    permits RespRequest.GetRequest, RespRequest.SetRequest, RespRequest.DeleteRequest {

  private final RespRequestCode requestCode;

  private RespRequest(RespRequestCode requestCode) {
    this.requestCode = requestCode;
  }

  public static RespRequest createFromRespArray(RespArray respArray) {
    try {
      List<RespElement> elements = respArray.getValue();
      RespRequestCode statusCode =
          RespRequestCode.fromValue((int) elements.get(0).getAsRespInteger().getValue());
      List<RespElement> subElements = elements.subList(1, elements.size());
      return switch (statusCode) {
        case GET -> new GetRequest(subElements);
        case SET -> new SetRequest(subElements);
        case DELETE -> new DeleteRequest(subElements);
      };
    } catch (Exception e) {
      throw new RespRequestConversionException(
          String.format("Failed to convert [%s] to a RespRequest.", respArray), e);
    }
  }

  /** The {@link RespRequestCode} indicating the type of request. */
  public RespRequestCode getRequestCode() {
    return requestCode;
  }

  /** Converts the request into a {@link RespArray} suitable for sending to a Bitflask server. */
  public abstract RespArray getAsRespArray();

  /** A request to get the value of the provided key. */
  public static final class GetRequest extends RespRequest {

    private final String key;

    public GetRequest(String key) {
      super(RespRequestCode.GET);
      this.key = key;
    }

    private GetRequest(List<RespElement> elements) {
      this(elements.getFirst().getAsRespBulkString().toString());
    }

    public String getKey() {
      return key;
    }

    @Override
    public RespArray getAsRespArray() {
      return new RespArray(
          ImmutableList.of(
              new RespInteger(getRequestCode().getValue()), new RespBulkString(getKey())));
    }
  }

  /** A request to set the {@code key} to {@code value}. */
  public static final class SetRequest extends RespRequest {

    private final String key;
    private final String value;

    public SetRequest(String key, String value) {
      super(RespRequestCode.SET);
      this.key = key;
      this.value = value;
    }

    private SetRequest(List<RespElement> elements) {
      this(
          elements.get(0).getAsRespBulkString().toString(),
          elements.get(1).getAsRespBulkString().toString());
    }

    public String getKey() {
      return key;
    }

    public String getValue() {
      return value;
    }

    @Override
    public RespArray getAsRespArray() {
      return new RespArray(
          ImmutableList.of(
              new RespInteger(getRequestCode().getValue()),
              new RespBulkString(getKey()),
              new RespBulkString(getValue())));
    }
  }

  /** A request to delete the {@code key} and its associated value. */
  public static final class DeleteRequest extends RespRequest {

    private final String key;

    public DeleteRequest(String key) {
      super(RespRequestCode.DELETE);
      this.key = key;
    }

    private DeleteRequest(List<RespElement> elements) {
      this(elements.getFirst().getAsRespBulkString().toString());
    }

    public String getKey() {
      return key;
    }

    @Override
    public RespArray getAsRespArray() {
      return new RespArray(
          ImmutableList.of(
              new RespInteger(getRequestCode().getValue()), new RespBulkString(getKey())));
    }
  }
}

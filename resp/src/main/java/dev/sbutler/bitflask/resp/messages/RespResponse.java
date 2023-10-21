package dev.sbutler.bitflask.resp.messages;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespInteger;
import java.util.List;
import java.util.Objects;

/** A response sent by a Bitflask server when using its RESP based API. */
public abstract sealed class RespResponse
    permits RespResponse.Failure,
        RespResponse.NoKnownLeader,
        RespResponse.NotCurrentLeader,
        RespResponse.Success {

  private final RespResponseCode responseCode;
  private final String message;

  private RespResponse(RespResponseCode responseCode, String message) {
    this.responseCode = responseCode;
    this.message = message;
  }

  /**
   * Creates an implementation of {@link RespResponse} from the provided {@link RespArray}.
   *
   * <p>A {@link RespResponseConversionException} will be thrown if the provided RespArray cannot be
   * converted.
   */
  public static RespResponse createFromRespArray(RespArray respArray) {
    try {
      List<RespElement> elements = respArray.getValue();
      RespResponseCode statusCode =
          RespResponseCode.fromValue((int) elements.get(0).getAsRespInteger().getValue());
      String message = elements.get(1).getAsRespBulkString().getValue();
      List<RespElement> subElements = elements.subList(2, elements.size());
      return switch (statusCode) {
        case SUCCESS -> new Success(message);
        case FAILURE -> new Failure(message);
        case NOT_CURRENT_LEADER -> new NotCurrentLeader(message, subElements);
        case NO_KNOWN_LEADER -> new NoKnownLeader(message);
      };
    } catch (Exception e) {
      throw new RespResponseConversionException(
          String.format("Failed to convert %s to a RespResponse.", respArray), e);
    }
  }

  /** The {@link RespResponseCode} indicating the type of response. */
  public RespResponseCode getResponseCode() {
    return responseCode;
  }

  /** A message from the server whose meaning various based on the type of response. */
  public String getMessage() {
    return message;
  }

  /** Converts the response into a {@link RespArray} suitable for sending to a Bitflask server. */
  public RespArray getAsRespArray() {
    return new RespArray(
        ImmutableList.of(
            new RespInteger(getResponseCode().getValue()), new RespBulkString(getMessage())));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof RespResponse that)) return false;
    return responseCode == that.responseCode && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(responseCode, message);
  }

  /** Indicates the Bitflask successfully processed the request. */
  public static final class Success extends RespResponse {
    public Success(String message) {
      super(RespResponseCode.SUCCESS, message);
    }
  }

  /** Indicates the Bitflask failed to process the request. */
  public static final class Failure extends RespResponse {
    public Failure(String message) {
      super(RespResponseCode.FAILURE, message);
    }
  }

  /**
   * Indicates the contacted Bitflask server is not the current leader and cannot process requests.
   *
   * <p>The known leader's hostname and resp port is included for retrying the request.
   */
  public static final class NotCurrentLeader extends RespResponse {

    private final String host;
    private final int respPort;

    public NotCurrentLeader(String host, int respPort) {
      super(
          RespResponseCode.NOT_CURRENT_LEADER,
          String.format("Current leader: [host=%s, respPort=%s].", host, respPort));
      this.host = host;
      this.respPort = respPort;
    }

    private NotCurrentLeader(String message, List<RespElement> elements) {
      super(RespResponseCode.NOT_CURRENT_LEADER, message);
      this.host = elements.get(0).getAsRespBulkString().getValue();
      this.respPort = (int) elements.get(1).getAsRespInteger().getValue();
    }

    /** The current Bitflask server leader's hostname. */
    public String getHost() {
      return host;
    }

    /** The current Bitflask server leader's resp port. */
    public int getRespPort() {
      return respPort;
    }

    @Override
    public RespArray getAsRespArray() {
      List<RespElement> baseElements = super.getAsRespArray().getValue();
      return new RespArray(
          ImmutableList.<RespElement>builder()
              .addAll(baseElements)
              .add(new RespBulkString(getHost()))
              .add(new RespInteger(getRespPort()))
              .build());
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof NotCurrentLeader that)) return false;
      if (!super.equals(o)) return false;
      return respPort == that.respPort && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), host, respPort);
    }
  }

  /** Indicates the Bitflask server does not know the current leader server. */
  public static final class NoKnownLeader extends RespResponse {
    public NoKnownLeader() {
      super(RespResponseCode.NO_KNOWN_LEADER, "No leader is currently known.");
    }

    private NoKnownLeader(String message) {
      super(RespResponseCode.NO_KNOWN_LEADER, message);
    }
  }
}

package dev.sbutler.bitflask.resp.messages;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespInteger;
import java.util.List;

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

  public static RespResponse createFromRespArray(RespArray respArray) {
    try {
      List<RespElement> elements = respArray.getValue();
      RespResponseCode statusCode =
          RespResponseCode.fromValue((int) elements.get(0).getAsRespInteger().getValue());
      String message = elements.get(1).getAsRespBulkString().getValue();
      return switch (statusCode) {
        case SUCCESS -> new Success(message);
        case FAILURE -> new Failure(message);
        case NOT_CURRENT_LEADER -> new NotCurrentLeader(message, elements);
        case NO_KNOWN_LEADER -> new NoKnownLeader(message);
      };
    } catch (Exception e) {
      throw new RespResponseConversionException(
          String.format("Failed to convert [%s] to a RespResponse.", respArray), e);
    }
  }

  public RespResponseCode getResponseCode() {
    return responseCode;
  }

  public String getMessage() {
    return message;
  }

  public RespArray getAsRespArray() {
    return new RespArray(
        ImmutableList.of(
            new RespInteger(getResponseCode().getValue()), new RespBulkString(getMessage())));
  }

  public static final class Success extends RespResponse {
    public Success(String message) {
      super(RespResponseCode.SUCCESS, message);
    }
  }

  public static final class Failure extends RespResponse {
    public Failure(String message) {
      super(RespResponseCode.FAILURE, message);
    }
  }

  public static final class NotCurrentLeader extends RespResponse {

    private final String host;
    private final int port;

    public NotCurrentLeader(String host, int port) {
      super(
          RespResponseCode.NOT_CURRENT_LEADER,
          String.format("Current leader: host %s, port %s.", host, port));
      this.host = host;
      this.port = port;
    }

    private NotCurrentLeader(String message, List<RespElement> elements) {
      super(RespResponseCode.NOT_CURRENT_LEADER, message);
      this.host = elements.get(2).getAsRespBulkString().getValue();
      this.port = (int) elements.get(3).getAsRespInteger().getValue();
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    @Override
    public RespArray getAsRespArray() {
      List<RespElement> baseElements = super.getAsRespArray().getValue();
      return new RespArray(
          ImmutableList.<RespElement>builder()
              .addAll(baseElements)
              .add(new RespBulkString(getHost()))
              .add(new RespInteger(getPort()))
              .build());
    }
  }

  public static final class NoKnownLeader extends RespResponse {
    public NoKnownLeader(String message) {
      super(RespResponseCode.NO_KNOWN_LEADER, message);
    }
  }
}

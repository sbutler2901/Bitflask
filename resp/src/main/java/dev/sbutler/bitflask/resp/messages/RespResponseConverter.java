package dev.sbutler.bitflask.resp.messages;

import com.google.common.base.Converter;
import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.resp.types.RespArray;
import dev.sbutler.bitflask.resp.types.RespBulkString;
import dev.sbutler.bitflask.resp.types.RespElement;
import dev.sbutler.bitflask.resp.types.RespInteger;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Handles converting between {@link RespResponse} and a {@link RespArray} representing a
 * RespResponse.
 */
public class RespResponseConverter extends Converter<RespResponse, RespArray> {

  public static final RespResponseConverter INSTANCE = new RespResponseConverter();

  @Override
  protected RespArray doForward(@NonNull RespResponse respResponse) {
    ImmutableList<RespElement> responseList =
        ImmutableList.of(
            new RespInteger(respResponse.statusCode().getValue()),
            new RespBulkString(respResponse.message()));

    return new RespArray(responseList);
  }

  @Override
  protected RespResponse doBackward(@NonNull RespArray respElement) {
    try {
      List<RespElement> responseArray = respElement.getValue();
      RespStatusCode statusCode =
          RespStatusCode.fromValue((int) responseArray.get(0).getAsRespInteger().getValue());
      String message = responseArray.get(1).getAsRespBulkString().getValue();
      return new RespResponse(statusCode, message);
    } catch (Exception e) {
      throw new RespResponseConversionException(
          String.format("Failed to convert [%s] to a RespResponse.", respElement), e);
    }
  }

  private RespResponseConverter() {}
}

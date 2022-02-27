package dev.sbutler.bitflask.resp.network.writer;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.IOException;

public interface RespWriter {

  void writeRespType(RespType<?> respType) throws IOException;
}

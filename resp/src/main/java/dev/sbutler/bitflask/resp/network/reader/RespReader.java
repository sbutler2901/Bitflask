package dev.sbutler.bitflask.resp.network.reader;

import dev.sbutler.bitflask.resp.types.RespType;
import java.io.IOException;

public interface RespReader {

  RespType<?> readNextRespType() throws IOException;
}

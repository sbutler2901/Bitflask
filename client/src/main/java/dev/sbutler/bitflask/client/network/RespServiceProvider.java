package dev.sbutler.bitflask.client.network;

import com.google.inject.throwingproviders.CheckedProvider;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;

public interface RespServiceProvider extends CheckedProvider<RespService> {

  RespService get() throws IOException;
}

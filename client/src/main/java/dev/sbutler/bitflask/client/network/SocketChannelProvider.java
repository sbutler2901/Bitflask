package dev.sbutler.bitflask.client.network;

import com.google.inject.throwingproviders.CheckedProvider;
import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface SocketChannelProvider extends CheckedProvider<SocketChannel> {

  SocketChannel get() throws IOException;
}

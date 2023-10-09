package dev.sbutler.bitflask.resp.network;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
public final class RespServiceProvider implements Provider<RespService> {

  private volatile RespService respService;

  @Override
  public RespService get() {
    return respService;
  }

  public void updateRespService(RespService respService) {
    this.respService = respService;
  }
}

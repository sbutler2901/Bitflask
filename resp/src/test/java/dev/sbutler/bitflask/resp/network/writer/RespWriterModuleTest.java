package dev.sbutler.bitflask.resp.network.writer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;

public class RespWriterModuleTest {

  private final RespWriterModule respNetworkModule = new RespWriterModule();

  @Test
  void provideRespWriter() {
    RespWriterImpl respWriterImpl = mock(RespWriterImpl.class);
    RespWriter respWriter = respNetworkModule.provideRespWriter(respWriterImpl);
    assertEquals(respWriterImpl, respWriter);
    assertInstanceOf(RespWriterImpl.class, respWriter);
  }

}

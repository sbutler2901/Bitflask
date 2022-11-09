package dev.sbutler.bitflask.client;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.client.configuration.ClientConfiguration;
import dev.sbutler.bitflask.resp.network.RespService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClientModuleTest {

  ClientModule clientModule;
  ClientConfiguration configuration;
  RespService respService;

  @BeforeEach
  void beforeEach() {
    configuration = mock(ClientConfiguration.class);
    respService = mock(RespService.class);
    clientModule = ClientModule.create(configuration, respService);
  }

  @Test
  void configure() {
    Injector injector = Guice.createInjector(clientModule);
    // Act / Assert
    injector.getBinding(ClientConfiguration.class);
    injector.getBinding(RespService.class);
  }

  @Test
  void provideClientConfiguration() {
    // Act
    ClientConfiguration providedConfiguration = clientModule.provideClientConfiguration();
    // Assert
    assertThat(providedConfiguration).isEqualTo(configuration);
  }

  @Test
  void provideRespService() {
    // Act
    RespService providedRespService = clientModule.provideRespService();
    // Assert
    assertThat(providedRespService).isEqualTo(respService);
  }
}

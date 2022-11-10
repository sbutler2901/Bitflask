package dev.sbutler.bitflask.server.network_service.client_handling_service;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import dev.sbutler.bitflask.resp.network.RespService;
import dev.sbutler.bitflask.server.command_processing_service.CommandProcessingService;
import javax.inject.Provider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ClientHandlingServiceChildModuleTest {

  @InjectMocks
  private ClientHandlingServiceChildModule clientHandlingServiceChildModule;

  @Mock
  private RespService respService;

  @Test
  void configure() {
    Injector injector = Guice.createInjector(
        new MockModule(),
        clientHandlingServiceChildModule);
    try {
      injector.getBinding(RespService.class);
      injector.getBinding(ClientMessageProcessor.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void provideRespService() {
    // Act
    RespService providedRespService = clientHandlingServiceChildModule.provideRespService();
    // Assert
    assertThat(providedRespService).isEqualTo(respService);
  }

  private static class MockModule extends AbstractModule {

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
      bind(CommandProcessingService.class).toProvider(mock(Provider.class));
    }
  }
}

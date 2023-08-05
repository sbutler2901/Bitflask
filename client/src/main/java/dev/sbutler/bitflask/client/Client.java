package dev.sbutler.bitflask.client;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import dev.sbutler.bitflask.client.client_processing.ReplClientProcessorService;
import dev.sbutler.bitflask.resp.network.RespService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import picocli.CommandLine;

@CommandLine.Command(
    name = "Client",
    mixinStandardHelpOptions = true,
    version = "Client 1.0",
    description = "Client for interacting with a Bitflask server using the RespProtocol")
public final class Client implements Callable<Integer> {

  @CommandLine.Option(
      names = {"-h", "--host"},
      description = "The RESP port of the Bitflask server")
  String host = "localhost";

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "The RESP port of the Bitflask server")
  int port = 9090;

  public static void main(String[] args) {
    int exitCode = new CommandLine(new Client()).execute(args);
    System.exit(exitCode);
  }

  @Override
  public Integer call() {
    try {
      printConfigInfo();

      RespService respService =
          RespService.create(SocketChannel.open(new InetSocketAddress(host, port)));

      Injector injector =
          Guice.createInjector(
              new ClientModule.Builder()
                  .addRuntimeModule(
                      new AbstractModule() {
                        @Override
                        protected void configure() {
                          super.configure();
                          bind(RespService.class).toInstance(respService);
                        }
                      })
                  .build());

      ReplClientProcessorService replClientProcessorService =
          injector.getInstance(ReplClientProcessorService.class);

      registerShutdownHook(replClientProcessorService, respService);

      replClientProcessorService.run();
    } catch (ProvisionException e) {
      e.getErrorMessages().forEach(System.err::println);
      return 1;
    } catch (ConfigurationException e) {
      e.getErrorMessages().forEach(System.err::println);
      return 1;
    } catch (Exception e) {
      e.printStackTrace();
      return 1;
    }
    return 0;
  }

  private void registerShutdownHook(
      ReplClientProcessorService replClientProcessorService, RespService respService) {
    Runtime.getRuntime()
        .addShutdownHook(
            Thread.ofVirtual()
                .unstarted(
                    () -> {
                      System.out.println("Exiting...");
                      replClientProcessorService.triggerShutdown();
                      closeConnection(respService);
                    }));
  }

  private void closeConnection(RespService respService) {
    try {
      respService.close();
    } catch (IOException e) {
      System.err.println("Issues closing connection " + e);
    }
  }

  private void printConfigInfo() {
    System.out.println("Using java version " + System.getProperty("java.version"));
    System.out.println(
        "Runtime processors available " + Runtime.getRuntime().availableProcessors());
    System.out.printf("Host [%s], port [%d]%n", host, port);
  }
}

package dev.sbutler.bitflask.client.client_processing;

public interface ClientProcessorService extends Runnable {

  void triggerShutdown();
}

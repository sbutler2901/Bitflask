package dev.sbutler.bitflask.raft;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The main topic for notifying {@link dev.sbutler.bitflask.raft.RaftCommandObserver}s of a new
 * committed command.
 */
@Singleton
final class RaftCommandTopic implements RaftCommandSubject {

  private final Set<RaftCommandObserver> observers = new CopyOnWriteArraySet<>();

  @Inject
  RaftCommandTopic() {}

  @Override
  public void notifyObservers(RaftCommand raftCommand) {
    for (var observer : observers) {
      observer.acceptRaftCommand(raftCommand);
    }
  }

  @Override
  public void register(RaftCommandObserver observer) {
    observers.add(observer);
  }

  @Override
  public void unregister(RaftCommandObserver observer) {
    observers.remove(observer);
  }
}

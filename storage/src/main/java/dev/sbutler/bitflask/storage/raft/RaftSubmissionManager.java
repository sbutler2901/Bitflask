package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Singleton;
import dev.sbutler.bitflask.storage.commands.StorageCommandResults;
import jakarta.inject.Inject;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListSet;

/** Manages submissions to Raft that must complete before responding to clients. */
@Singleton
final class RaftSubmissionManager {

  private final NavigableSet<WaitingSubmission> waitingSubmissions = new ConcurrentSkipListSet<>();

  @Inject
  RaftSubmissionManager() {}

  ListenableFuture<StorageCommandResults> addNewSubmission(int newEntryIndex) {
    SettableFuture<StorageCommandResults> clientSubmitFuture = SettableFuture.create();
    waitingSubmissions.add(new WaitingSubmission(newEntryIndex, clientSubmitFuture));
    return clientSubmitFuture;
  }

  void completeSubmission(int entryIndex, StorageCommandResults results) {
    Preconditions.checkState(
        !waitingSubmissions.isEmpty(), "There are no waiting submission to complete");
    WaitingSubmission firstSubmission = waitingSubmissions.first();
    Preconditions.checkState(
        firstSubmission.entryIndex() == entryIndex,
        String.format(
            "The provided index [%d] does not match the first waiting submissions index [%d].",
            entryIndex, firstSubmission.entryIndex()));

    firstSubmission = waitingSubmissions.pollFirst();
    firstSubmission.submissionFuture().set(results);
  }

  void completeAllSubmissionsWithFailure(Exception failure) {
    for (var waitingSubmission : waitingSubmissions) {
      waitingSubmission.submissionFuture().setException(failure);
      waitingSubmissions.remove(waitingSubmission);
    }
  }

  /** Holds a submission future that cannot be resolved until the associated entry is applied. */
  private record WaitingSubmission(
      int entryIndex, SettableFuture<StorageCommandResults> submissionFuture)
      implements Comparable<WaitingSubmission> {

    @Override
    public int compareTo(WaitingSubmission waitingSubmission) {
      return Integer.compare(this.entryIndex(), waitingSubmission.entryIndex());
    }
  }
}

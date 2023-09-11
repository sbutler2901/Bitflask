package dev.sbutler.bitflask.storage.raft;

import com.google.common.base.Preconditions;
import com.google.common.flogger.FluentLogger;
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

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final NavigableSet<WaitingSubmission> waitingSubmissions = new ConcurrentSkipListSet<>();

  @Inject
  RaftSubmissionManager() {}

  /** Adds a new submission for an Entry at the provided index. */
  ListenableFuture<StorageCommandResults> addNewSubmission(int newEntryIndex) {
    SettableFuture<StorageCommandResults> clientSubmitFuture = SettableFuture.create();
    waitingSubmissions.add(new WaitingSubmission(newEntryIndex, clientSubmitFuture));
    logger.atInfo().log("Added new submission for Entry at index [%d].", newEntryIndex);
    return clientSubmitFuture;
  }

  /** Completes the submission with {@code results} for the Entry at {@code entryIndex}. */
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
    logger.atInfo().log("Completed submission for Entry at index [%d].", entryIndex);
  }

  void completeAllSubmissionsWithFailure(Exception failure) {
    logger.atWarning().withCause(failure).log(
        "Completing all [%d] waiting submissions with failure.", waitingSubmissions.size());
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

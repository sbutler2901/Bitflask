package dev.sbutler.bitflask.storage.raft;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.sbutler.bitflask.storage.commands.StorageCommandResults;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RaftSubmissionManager}. */
public class RaftSubmissionManagerTest {

  private final RaftSubmissionManager submissionManager = new RaftSubmissionManager();

  @Test
  public void completeSubmission() {
    var resultsFuture = submissionManager.addNewSubmission(1);
    assertThat(resultsFuture.isDone()).isFalse();
    var results = new StorageCommandResults.Success("success");

    submissionManager.completeSubmission(1, results);

    assertThat(resultsFuture.isDone()).isTrue();
    assertThat(resultsFuture.resultNow()).isEqualTo(results);
  }

  @Test
  public void completeSubmission_emptySubmission_throwsIllegalStateException() {
    var results = new StorageCommandResults.Success("success");

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> submissionManager.completeSubmission(0, results));

    assertThat(exception).hasMessageThat().isEqualTo("There are no waiting submission to complete");
  }

  @Test
  public void completeSubmission_outOfOrderSubmission_throwsIllegalStateException() {
    submissionManager.addNewSubmission(1);
    var results = new StorageCommandResults.Success("success");

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class, () -> submissionManager.completeSubmission(2, results));

    assertThat(exception)
        .hasMessageThat()
        .isEqualTo(
            "The provided index [2] does not match the first waiting submissions index [1].");
  }

  @Test
  public void completeAllSubmissionsWithFailure() {
    var resultsFuture = submissionManager.addNewSubmission(1);
    assertThat(resultsFuture.isDone()).isFalse();
    RuntimeException exception = new RuntimeException("test");

    submissionManager.completeAllSubmissionsWithFailure(exception);

    assertThat(resultsFuture.isDone()).isTrue();
    assertThat(resultsFuture.exceptionNow()).isEqualTo(exception);
  }
}

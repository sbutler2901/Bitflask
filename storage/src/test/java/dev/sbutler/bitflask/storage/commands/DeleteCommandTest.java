package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.DeleteDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.Segment;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteCommandTest {

  DeleteCommand command;

  @Mock
  SegmentManagerService segmentManagerService;
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  String key = "key";
  DeleteDTO dto = new DeleteDTO(key);

  @Mock
  Segment writable;

  @BeforeEach
  void beforeEach() {
    when(segmentManagerService.getWritableSegment()).thenReturn(writable);
    command = new DeleteCommand(executorService, segmentManagerService, dto);
  }

  @Test
  void success() throws Exception {
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Success.class, responseFuture.get());
    Success response = (Success) responseFuture.get();
    assertEquals("OK", response.message());
  }

  @Test
  void failed() throws Exception {
    // Arrange
    doThrow(IOException.class).when(writable).delete(key);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Failed.class, responseFuture.get());
    Failed response = (Failed) responseFuture.get();
    assertTrue(response.message().toLowerCase().contains("failure"));
  }
}

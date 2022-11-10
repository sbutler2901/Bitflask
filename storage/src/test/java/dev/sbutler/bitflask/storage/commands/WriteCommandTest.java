package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.WriteDTO;
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
public class WriteCommandTest {

  WriteCommand command;

  @Mock
  SegmentManagerService segmentManagerService;
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  String key = "key", value = "value";
  WriteDTO dto = new WriteDTO(key, value);

  @Mock
  Segment writable;

  @BeforeEach
  void beforeEach() {
    when(segmentManagerService.getWritableSegment()).thenReturn(writable);
    command = new WriteCommand(executorService, segmentManagerService, dto);
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
    doThrow(IOException.class).when(writable).write(key, value);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Failed.class, responseFuture.get());
    Failed response = (Failed) responseFuture.get();
    assertTrue(response.message().toLowerCase().contains("failure"));
  }
}

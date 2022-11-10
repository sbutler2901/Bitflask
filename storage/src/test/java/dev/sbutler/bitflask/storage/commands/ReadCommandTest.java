package dev.sbutler.bitflask.storage.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.testing.TestingExecutors;
import dev.sbutler.bitflask.storage.dispatcher.StorageCommandDTO.ReadDTO;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Failed;
import dev.sbutler.bitflask.storage.dispatcher.StorageResponse.Success;
import dev.sbutler.bitflask.storage.segment.Segment;
import dev.sbutler.bitflask.storage.segment.SegmentManagerService;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReadCommandTest {

  ReadCommand command;

  @Mock
  SegmentManagerService segmentManagerService;
  @SuppressWarnings("UnstableApiUsage")
  ListeningExecutorService executorService = TestingExecutors.sameThreadScheduledExecutor();
  String key = "key", value = "value";
  ReadDTO dto = new ReadDTO(key);

  @Mock
  Segment writable;
  @Mock
  Segment frozen;

  @BeforeEach
  void beforeEach() {
    when(segmentManagerService.getReadableSegments())
        .thenReturn(ImmutableList.of(writable, frozen));
    command = new ReadCommand(executorService, segmentManagerService, dto);
  }

  @Test
  void keyFound_writeableSegment() throws Exception {
    // Arrange
    doReturn(true).when(writable).containsKey(key);
    doReturn(Optional.of(value)).when(writable).read(key);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Success.class, responseFuture.get());
    Success response = (Success) responseFuture.get();
    assertEquals(value, response.message());
  }

  @Test
  void keyFound_frozenSegments() throws Exception {
    // Arrange
    doReturn(false).when(writable).containsKey(key);
    doReturn(true).when(frozen).containsKey(key);
    doReturn(Optional.of(value)).when(frozen).read(key);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Success.class, responseFuture.get());
    Success response = (Success) responseFuture.get();
    assertEquals(value, response.message());
  }

  @Test
  void keyNotFound() throws Exception {
    // Arrange
    doReturn(false).when(writable).containsKey(anyString());
    doReturn(false).when(frozen).containsKey(anyString());
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Success.class, responseFuture.get());
    Success response = (Success) responseFuture.get();
    assertTrue(response.message().contains("not found"));
  }

  @Test
  void readException() throws Exception {
    // Arrange
    doReturn(true).when(writable).containsKey(key);
    doThrow(IOException.class).when(writable).read(key);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertInstanceOf(Failed.class, responseFuture.get());
    Failed response = (Failed) responseFuture.get();
    assertTrue(response.message().toLowerCase().contains("failure"));
  }
}

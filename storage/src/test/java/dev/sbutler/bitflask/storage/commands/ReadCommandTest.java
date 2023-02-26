package dev.sbutler.bitflask.storage.commands;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
import dev.sbutler.bitflask.storage.segmentV1.Segment;
import dev.sbutler.bitflask.storage.segmentV1.SegmentManagerService;
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
    when(writable.containsKey(anyString())).thenReturn(true);
    when(writable.read(anyString())).thenReturn(Optional.of(value));
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertThat(responseFuture.get()).isInstanceOf(Success.class);
    Success response = (Success) responseFuture.get();
    assertThat(response.message()).isEqualTo(value);
  }

  @Test
  void keyFound_frozenSegments() throws Exception {
    // Arrange
    when(writable.containsKey(anyString())).thenReturn(false);
    when(frozen.containsKey(anyString())).thenReturn(true);
    when(frozen.read(anyString())).thenReturn(Optional.of(value));
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertThat(responseFuture.get()).isInstanceOf(Success.class);
    Success response = (Success) responseFuture.get();
    assertThat(response.message()).isEqualTo(value);
  }

  @Test
  void keyNotFound() throws Exception {
    // Arrange
    when(writable.containsKey(anyString())).thenReturn(false);
    when(frozen.containsKey(anyString())).thenReturn(false);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertThat(responseFuture.get()).isInstanceOf(Success.class);
    Success response = (Success) responseFuture.get();
    assertThat(response.message()).ignoringCase().contains("not found");
  }

  @Test
  void readException() throws Exception {
    // Arrange
    when(writable.containsKey(anyString())).thenReturn(true);
    doThrow(IOException.class).when(writable).read(key);
    // Act
    ListenableFuture<StorageResponse> responseFuture = command.execute();
    // Assert
    assertThat(responseFuture.get()).isInstanceOf(Failed.class);
    Failed response = (Failed) responseFuture.get();
    assertThat(response.message()).ignoringCase().contains("failure to read");
  }
}

package dev.sbutler.bitflask.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class StorageSegmentTest {

  @Test
  @SuppressWarnings("unchecked")
  void write() throws IOException {
    String key = "key", value0 = "value0", value1 = "value1";

    ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
    Future<Integer> writeFuture = mock(Future.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ThreadPoolExecutor.class)))
          .thenReturn(asynchronousFileChannel);
      doReturn(writeFuture).when(asynchronousFileChannel).write(any(ByteBuffer.class), anyLong());

      StorageSegment storageSegment = new StorageSegment(threadPoolExecutor, 0);

      storageSegment.write(key, value0);
      assertFalse(storageSegment.exceedsStorageThreshold());
      assertTrue(storageSegment.containsKey(key));

      // later merge
      storageSegment.write(key, value1);
      assertFalse(storageSegment.exceedsStorageThreshold());
      assertTrue(storageSegment.containsKey(key));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void write_Exception() throws IOException, ExecutionException, InterruptedException {
    String key = "key", value = "value";
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ThreadPoolExecutor.class)))
          .thenReturn(asynchronousFileChannel);
      Future<Integer> writeFuture = mock(Future.class);
      doReturn(writeFuture).when(asynchronousFileChannel).write(any(ByteBuffer.class), anyLong());
      doThrow(new InterruptedException("test: interruptException")).when(writeFuture).get();

      StorageSegment storageSegment = new StorageSegment(threadPoolExecutor, 0);
      storageSegment.write(key, value);

      assertFalse(storageSegment.exceedsStorageThreshold());
      assertFalse(storageSegment.containsKey(key));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void read() throws IOException, ExecutionException, InterruptedException {
    String key = "key", value = "value", combined = key + value;
    byte[] combinedBytes = combined.getBytes(StandardCharsets.UTF_8);

    ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
    Future<Integer> writeFuture = mock(Future.class);
    Future<Integer> readFuture = mock(Future.class);
    ByteBuffer byteBuffer = mock(ByteBuffer.class);
    try (
        MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
            AsynchronousFileChannel.class);
        MockedStatic<ByteBuffer> byteBufferMockedStatic = mockStatic(ByteBuffer.class)
    ) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ThreadPoolExecutor.class)))
          .thenReturn(asynchronousFileChannel);
      byteBufferMockedStatic.when(() -> ByteBuffer.allocate(anyInt())).thenReturn(byteBuffer);
      doReturn(writeFuture).when(asynchronousFileChannel).write(any(), anyLong());
      doReturn(readFuture).when(asynchronousFileChannel).read(any(), anyLong());
      doReturn(combinedBytes.length).when(readFuture).get();
      doReturn(combinedBytes).when(byteBuffer).array();

      StorageSegment storageSegment = new StorageSegment(threadPoolExecutor, 0);
      // write first to ensure key exists
      storageSegment.write(key, value);

      Optional<String> result = storageSegment.read(key);

      assertTrue(result.isPresent());
      assertEquals(value, result.get());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void read_notEnoughBytesRead() throws IOException, ExecutionException, InterruptedException {
    String key = "key", value = "value", combined = key + value;
    byte[] combinedBytes = combined.getBytes(StandardCharsets.UTF_8);

    ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
    Future<Integer> readFuture = mock(Future.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ThreadPoolExecutor.class)))
          .thenReturn(asynchronousFileChannel);
      doReturn(readFuture).when(asynchronousFileChannel).read(any(ByteBuffer.class), anyLong());
      doReturn(combinedBytes.length - 1).when(readFuture).get();

      StorageSegment storageSegment = new StorageSegment(threadPoolExecutor, 0);
      // write first to ensure key exists
      doReturn(mock(Future.class)).when(asynchronousFileChannel)
          .write(any(ByteBuffer.class), anyLong());
      storageSegment.write(key, value);

      assertTrue(storageSegment.read(key).isEmpty());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void read_exception() throws Exception {
    String key = "key", value = "value";

    ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
    Future<Integer> writeFuture = mock(Future.class);
    Future<Integer> readFuture = mock(Future.class);
    try (
        MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
            AsynchronousFileChannel.class)
    ) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ThreadPoolExecutor.class)))
          .thenReturn(asynchronousFileChannel);
      doReturn(writeFuture).when(asynchronousFileChannel).write(any(), anyLong());
      doReturn(readFuture).when(asynchronousFileChannel).read(any(), anyLong());
      doThrow(new InterruptedException("test: interruptException")).when(readFuture).get();

      StorageSegment storageSegment = new StorageSegment(threadPoolExecutor, 0);
      // write first to ensure key exists
      storageSegment.write(key, value);

      Optional<String> result = storageSegment.read(key);
      assertTrue(result.isEmpty());
    }
  }

  @Test
  void read_keyNotFound() throws IOException {
    String key = "key";

    ThreadPoolExecutor threadPoolExecutor = mock(ThreadPoolExecutor.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ThreadPoolExecutor.class)))
          .thenReturn(asynchronousFileChannel);

      StorageSegment storageSegment = new StorageSegment(threadPoolExecutor, 0);

      assertTrue(storageSegment.read(key).isEmpty());
    }
  }

}

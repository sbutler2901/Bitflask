package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class StorageSegmentFileTest {

  @Test
  void write() throws IOException {
    ExecutorService executorService = mock(ExecutorService.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      StorageSegmentFile storageSegmentFile = new StorageSegmentFile(executorService);

      doReturn(mock(Future.class)).when(asynchronousFileChannel)
          .write(any(ByteBuffer.class), anyLong());
      storageSegmentFile.write(new byte[]{'a'}, 0L);
      verify(asynchronousFileChannel, times(1)).write(any(ByteBuffer.class), anyLong());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void write_exception() throws IOException, ExecutionException, InterruptedException {
    ExecutorService executorService = mock(ExecutorService.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      StorageSegmentFile storageSegmentFile = new StorageSegmentFile(executorService);

      Future<Integer> writeFuture = mock(Future.class);
      doReturn(writeFuture).when(asynchronousFileChannel)
          .write(any(ByteBuffer.class), anyLong());
      doThrow(new InterruptedException("test: interruptException")).when(writeFuture).get();

      assertThrows(IOException.class,
          () -> storageSegmentFile.write(new byte[]{'a'}, 0L));
      verify(asynchronousFileChannel, times(1)).write(any(ByteBuffer.class), anyLong());
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void read() throws IOException {
    ExecutorService executorService = mock(ExecutorService.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      StorageSegmentFile storageSegmentFile = new StorageSegmentFile(executorService);

      Future<Integer> readFuture = mock(Future.class);
      doReturn(readFuture).when(asynchronousFileChannel).read(any(ByteBuffer.class), anyLong());

      byte[] result = storageSegmentFile.read(0, 0L);
      verify(asynchronousFileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
      assertEquals(0, result.length);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void read_exception() throws IOException, ExecutionException, InterruptedException {
    ExecutorService executorService = mock(ExecutorService.class);
    try (MockedStatic<AsynchronousFileChannel> asynchronousFileChannelMockedStatic = mockStatic(
        AsynchronousFileChannel.class)) {
      AsynchronousFileChannel asynchronousFileChannel = mock(AsynchronousFileChannel.class);
      asynchronousFileChannelMockedStatic.when(
              () -> AsynchronousFileChannel.open(any(Path.class), any(), any(ExecutorService.class)))
          .thenReturn(asynchronousFileChannel);

      StorageSegmentFile storageSegmentFile = new StorageSegmentFile(executorService);

      Future<Integer> readFuture = mock(Future.class);
      doReturn(readFuture).when(asynchronousFileChannel).read(any(ByteBuffer.class), anyLong());
      doThrow(new InterruptedException("test: interruptException")).when(readFuture).get();

      assertThrows(IOException.class, () -> storageSegmentFile.read(0, 0L));
      verify(asynchronousFileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
    }
  }
}

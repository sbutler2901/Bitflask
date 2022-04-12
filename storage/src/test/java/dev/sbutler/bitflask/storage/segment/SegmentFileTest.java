package dev.sbutler.bitflask.storage.segment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SegmentFileTest {

  @InjectMocks
  SegmentFile segmentFile;
  @Mock
  AsynchronousFileChannel asynchronousFileChannel;

  @Test
  void write() throws IOException {
    doReturn(mock(Future.class)).when(asynchronousFileChannel)
        .write(any(ByteBuffer.class), anyLong());
    segmentFile.write(new byte[]{'a'}, 0L);
    verify(asynchronousFileChannel, times(1)).write(any(ByteBuffer.class), anyLong());
  }

  @Test
  @SuppressWarnings("unchecked")
  void write_exception() throws ExecutionException, InterruptedException {
    Future<Integer> writeFuture = mock(Future.class);
    doReturn(writeFuture).when(asynchronousFileChannel)
        .write(any(ByteBuffer.class), anyLong());
    doThrow(new InterruptedException("test: interruptException")).when(writeFuture).get();

    assertThrows(IOException.class,
        () -> segmentFile.write(new byte[]{'a'}, 0L));
    verify(asynchronousFileChannel, times(1)).write(any(ByteBuffer.class), anyLong());
  }

  @Test
  @SuppressWarnings("unchecked")
  void read() throws IOException {
    Future<Integer> readFuture = mock(Future.class);
    doReturn(readFuture).when(asynchronousFileChannel).read(any(ByteBuffer.class), anyLong());

    byte[] result = segmentFile.read(0, 0L);
    verify(asynchronousFileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
    assertEquals(0, result.length);
  }

  @Test
  @SuppressWarnings("unchecked")
  void read_exception() throws ExecutionException, InterruptedException {
    Future<Integer> readFuture = mock(Future.class);
    doReturn(readFuture).when(asynchronousFileChannel).read(any(ByteBuffer.class), anyLong());
    doThrow(new InterruptedException("test: interruptException")).when(readFuture).get();

    assertThrows(IOException.class, () -> segmentFile.read(0, 0L));
    verify(asynchronousFileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
  }

  @Test
  @SuppressWarnings("unchecked")
  void readByte() throws IOException {
    Future<Integer> readFuture = mock(Future.class);

    doReturn(readFuture).when(asynchronousFileChannel).read(any(ByteBuffer.class), anyLong());

    byte result = segmentFile.readByte(0L);
    verify(asynchronousFileChannel, times(1)).read(any(ByteBuffer.class), anyLong());
    assertEquals(0, result);
  }

  @Test
  void size() throws IOException {
    doReturn(0L).when(asynchronousFileChannel).size();
    assertEquals(0L, segmentFile.size());
  }
}

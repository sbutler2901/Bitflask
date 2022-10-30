package dev.sbutler.bitflask.client.client_processing.repl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplElement;
import dev.sbutler.bitflask.client.client_processing.repl.types.ReplString;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class ReplParserTest {

  @Test
  void readNextLine() throws Exception {
    // Arrange
    String value = "set test value";
    ReplReader replReader = new ReplReader(new StringReader(value));
    // Act
    ImmutableList<ReplElement> elements = ReplParser.readNextLine(replReader);
    // Assert
    assertThat(elements).hasSize(3);
    ImmutableList<ReplElement> expected = ImmutableList.of(
        new ReplString("set"),
        new ReplString("test"),
        new ReplString("value")
    );
    assertThat(elements).containsExactlyElementsIn(expected).inOrder();
  }

  @Test
  void readNextLine_readingIoException_throwsReplIOException() throws Exception {
    // Arrange
    ReplReader replReader = mock(ReplReader.class);
    IOException ioException = new IOException("test");
    when(replReader.readToEndLine()).thenThrow(ioException);
    // Act
    ReplIOException exception =
        assertThrows(ReplIOException.class, () -> ReplParser.readNextLine(replReader));
    // Assert
    assertThat(exception).hasMessageThat().ignoringCase().contains("failed to read");
    assertThat(exception).hasCauseThat().isEqualTo(ioException);
  }
}

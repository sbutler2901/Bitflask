package dev.sbutler.bitflask.storage.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.UnsignedBytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import org.junit.jupiter.api.Test;

public class SegmentMetadataTest {

  @Test
  public void identityConversion_fromBytes() {
    UnsignedShort segmentNumber = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    UnsignedShort segmentLevel = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    byte[] expectedBytes = Bytes.concat(segmentNumber.getBytes(), segmentLevel.getBytes());

    byte[] bytes = SegmentMetadata.fromBytes(expectedBytes).getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void identityConversion_getBytes() {
    UnsignedShort segmentNumber = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    UnsignedShort segmentLevel = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    SegmentMetadata expected = new SegmentMetadata(segmentNumber, segmentLevel);

    SegmentMetadata created = SegmentMetadata.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void fromBytes_lowerRange() {
    byte[] bytes = new byte[]{0, 0, 0, 0};

    SegmentMetadata metadata = SegmentMetadata.fromBytes(bytes);

    assertThat(metadata.getSegmentNumber()).isEqualTo(UnsignedShort.MIN_VALUE);
    assertThat(metadata.getSegmentLevel()).isEqualTo(UnsignedShort.MIN_VALUE);
  }

  @Test
  public void fromBytes_upperRange() {
    byte unsignedMax = UnsignedBytes.checkedCast(255);
    byte[] bytes = new byte[]{unsignedMax, unsignedMax, unsignedMax, unsignedMax};

    SegmentMetadata metadata = SegmentMetadata.fromBytes(bytes);

    assertThat(metadata.getSegmentNumber()).isEqualTo(UnsignedShort.MAX_VALUE);
    assertThat(metadata.getSegmentLevel()).isEqualTo(UnsignedShort.MAX_VALUE);
  }

  @Test
  public void fromBytes_invalidLength_lessThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[]{SegmentMetadata.BYTE_ARRAY_LENGTH - 1};

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void fromBytes_invalidLength_greaterThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[]{SegmentMetadata.BYTE_ARRAY_LENGTH + 1};

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void getBytes_lowerRange() {
    UnsignedShort minValue = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    SegmentMetadata metadata = new SegmentMetadata(minValue, minValue);

    assertThat(metadata.getBytes()).isEqualTo(
        Bytes.concat(minValue.getBytes(), minValue.getBytes()));
  }

  @Test
  public void getBytes_upperRange() {
    UnsignedShort maxValue = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    SegmentMetadata metadata = new SegmentMetadata(maxValue, maxValue);

    assertThat(metadata.getBytes()).isEqualTo(
        Bytes.concat(maxValue.getBytes(), maxValue.getBytes()));
  }
}

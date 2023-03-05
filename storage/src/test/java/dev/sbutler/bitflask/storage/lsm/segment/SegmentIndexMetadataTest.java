package dev.sbutler.bitflask.storage.lsm.segment;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.common.primitives.UnsignedBytes;
import dev.sbutler.bitflask.common.primitives.UnsignedShort;
import org.junit.jupiter.api.Test;

public class SegmentIndexMetadataTest {

  @Test
  public void identityConversion_fromBytes() {
    UnsignedShort segmentNumber = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    byte[] expectedBytes = segmentNumber.getBytes();

    byte[] bytes = SegmentIndexMetadata.fromBytes(expectedBytes).getBytes();

    assertThat(bytes).isEqualTo(expectedBytes);
  }

  @Test
  public void identityConversion_getBytes() {
    UnsignedShort segmentNumber = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    SegmentIndexMetadata expected = new SegmentIndexMetadata(segmentNumber);

    SegmentIndexMetadata created = SegmentIndexMetadata.fromBytes(expected.getBytes());

    assertThat(created).isEqualTo(expected);
  }

  @Test
  public void fromBytes_lowerRange() {
    byte[] bytes = new byte[]{0, 0};

    SegmentIndexMetadata metadata = SegmentIndexMetadata.fromBytes(bytes);

    assertThat(metadata.getSegmentNumber()).isEqualTo(UnsignedShort.MIN_VALUE);
  }

  @Test
  public void fromBytes_upperRange() {
    byte unsignedMax = UnsignedBytes.checkedCast(255);
    byte[] bytes = new byte[]{unsignedMax, unsignedMax};

    SegmentIndexMetadata metadata = SegmentIndexMetadata.fromBytes(bytes);

    assertThat(metadata.getSegmentNumber()).isEqualTo(UnsignedShort.MAX_VALUE);
  }

  @Test
  public void fromBytes_invalidLength_lessThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[]{SegmentIndexMetadata.BYTES - 1};

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentIndexMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void fromBytes_invalidLength_greaterThan_throwsIllegalArgumentException() {
    byte[] bytes = new byte[]{SegmentIndexMetadata.BYTES + 1};

    IllegalArgumentException e =
        assertThrows(IllegalArgumentException.class, () -> SegmentIndexMetadata.fromBytes(bytes));

    assertThat(e).hasMessageThat().ignoringCase().contains("Byte array length invalid.");
  }

  @Test
  public void getBytes_lowerRange() {
    UnsignedShort minValue = UnsignedShort.valueOf(UnsignedShort.MIN_VALUE);
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(minValue);

    assertThat(metadata.getBytes()).isEqualTo(minValue.getBytes());
  }

  @Test
  public void getBytes_upperRange() {
    UnsignedShort maxValue = UnsignedShort.valueOf(UnsignedShort.MAX_VALUE);
    SegmentIndexMetadata metadata = new SegmentIndexMetadata(maxValue);

    assertThat(metadata.getBytes()).isEqualTo(maxValue.getBytes());
  }
}

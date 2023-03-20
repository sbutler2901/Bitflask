package dev.sbutler.bitflask.storage.lsm.entry;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.time.Instant;
import org.junit.jupiter.api.Test;

public class EntryUtilsTest {

  private static final long EPOCH_SECONDS_0 = Instant.now().getEpochSecond();
  private static final long EPOCH_SECONDS_1 = Instant.now().getEpochSecond();

  private static final Entry ENTRY_0 = new Entry(EPOCH_SECONDS_0, "key0", "value0");
  private static final Entry ENTRY_0_DUPLICATE_NEWER =
      new Entry(EPOCH_SECONDS_1, ENTRY_0.key(), ENTRY_0.value());
  private static final Entry ENTRY_1 = new Entry(EPOCH_SECONDS_1, "key1", "value1");
  private static final Entry ENTRY_1_DUPLICATE_OLDER =
      new Entry(EPOCH_SECONDS_0, ENTRY_1.key(), ENTRY_1.value());

  @Test
  public void buildKeyEntryMap_allUnique_inOrder() {
    ImmutableList<Entry> entries = ImmutableList.of(ENTRY_0, ENTRY_1);

    ImmutableSortedMap<String, Entry> keyEntryMap = EntryUtils.buildKeyEntryMap(entries);

    assertThat(keyEntryMap.values()).containsExactly(ENTRY_0, ENTRY_1).inOrder();
    assertThat(keyEntryMap.get(ENTRY_0.key())).isEqualTo(ENTRY_0);
    assertThat(keyEntryMap.get(ENTRY_1.key())).isEqualTo(ENTRY_1);
  }

  @Test
  public void buildKeyEntryMap_allUnique_reverseOrderOrder() {
    ImmutableList<Entry> entries = ImmutableList.of(ENTRY_1, ENTRY_0);

    ImmutableSortedMap<String, Entry> keyEntryMap = EntryUtils.buildKeyEntryMap(entries);

    assertThat(keyEntryMap.values()).containsExactly(ENTRY_0, ENTRY_1).inOrder();
    assertThat(keyEntryMap.get(ENTRY_0.key())).isEqualTo(ENTRY_0);
    assertThat(keyEntryMap.get(ENTRY_1.key())).isEqualTo(ENTRY_1);
  }

  @Test
  public void buildKeyEntryMap_withNewerDuplicateFirst() {
    ImmutableList<Entry> entries = ImmutableList.of(ENTRY_0_DUPLICATE_NEWER, ENTRY_0);

    ImmutableSortedMap<String, Entry> keyEntryMap = EntryUtils.buildKeyEntryMap(entries);

    assertThat(keyEntryMap.values()).containsExactly(ENTRY_0_DUPLICATE_NEWER);
    assertThat(keyEntryMap.get(ENTRY_0_DUPLICATE_NEWER.key())).isEqualTo(ENTRY_0_DUPLICATE_NEWER);
  }

  @Test
  public void buildKeyEntryMap_withOlderDuplicateFirst() {
    ImmutableList<Entry> entries = ImmutableList.of(ENTRY_1_DUPLICATE_OLDER, ENTRY_1);

    ImmutableSortedMap<String, Entry> keyEntryMap = EntryUtils.buildKeyEntryMap(entries);

    assertThat(keyEntryMap.values()).containsExactly(ENTRY_1);
    assertThat(keyEntryMap.get(ENTRY_1.key())).isEqualTo(ENTRY_1);
  }
}

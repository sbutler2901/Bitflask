package dev.sbutler.bitflask.storage.lsm.entry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Utility methods for working with {@link Entry}s.
 */
public final class EntryUtils {

  /**
   * Builds a mutable {@link SortedMap} of {@link Entry}s by their key
   *
   * <p>The most recent {@link Entry} will be kept when duplicate keys are encountered.
   */
  public static SortedMap<String, Entry> buildKeyEntryMap(
      ImmutableList<Entry> entries) {
    SortedMap<String, Entry> keyEntryMap = new TreeMap<>();
    for (var entry : entries) {
      Entry prevEntry = keyEntryMap.get(entry.key());
      if (prevEntry == null
          || prevEntry.creationEpochSeconds() < entry.creationEpochSeconds()) {
        keyEntryMap.put(entry.key(), entry);
      }
    }
    return keyEntryMap;
  }

  /**
   * Builds an {@link ImmutableSortedMap} of {@link Entry}s by their key
   *
   * <p>The most recent {@link Entry} will be kept when duplicate keys are encountered.
   */
  public static ImmutableSortedMap<String, Entry> buildImmutableKeyEntryMap(
      ImmutableList<Entry> entries) {
    return ImmutableSortedMap.copyOfSorted(buildKeyEntryMap(entries));
  }

  private EntryUtils() {

  }
}

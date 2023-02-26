package dev.sbutler.bitflask.storage.segment;

import com.google.common.collect.ImmutableSortedMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import dev.sbutler.bitflask.storage.entry.Entry;

/**
 * Handles the creation of a {@link Segment}.
 */
public final class SegmentFactory {

  public ListenableFuture<Segment> create(ImmutableSortedMap<String, Entry> keyEntryMap) {
    // TODO: implement
    return Futures.immediateFuture(null);
  }
}

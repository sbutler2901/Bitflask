package dev.sbutler.bitflask.storage.lsm.segment;

import javax.inject.Inject;

public final class SegmentLevelMultiMapLoader {

  @Inject
  SegmentLevelMultiMapLoader() {

  }

  public SegmentLevelMultiMap load() {
    // TODO: implement proper loading from disk
    return SegmentLevelMultiMap.create();
  }
}

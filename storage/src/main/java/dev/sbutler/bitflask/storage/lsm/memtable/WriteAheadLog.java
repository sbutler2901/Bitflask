package dev.sbutler.bitflask.storage.lsm.memtable;

/**
 * A write-ahead-log for entries stored in the {@link Memtable}.
 *
 * <p>All writes to a Memtable should be proceeded by writing to a WriteAheadLog. This enables
 * recovering Memtable data that has not been flushed to a
 * {@link dev.sbutler.bitflask.storage.lsm.segment.Segment} in the case of a crash.
 */
public class WriteAheadLog {

}

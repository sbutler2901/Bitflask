package dev.sbutler.bitflask.storage;

import com.google.common.util.concurrent.SettableFuture;

record StorageSubmission(StorageCommand command, SettableFuture<StorageResponse> response) {

}

package dev.sbutler.bitflask.common.dispatcher;

import com.google.common.util.concurrent.SettableFuture;

public record DispatcherSubmission<C, R>(C commandDTO, SettableFuture<R> responseFuture) {

}

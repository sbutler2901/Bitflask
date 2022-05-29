package dev.sbutler.bitflask.storage.configuration.concurrency;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface StorageExecutorService {

}

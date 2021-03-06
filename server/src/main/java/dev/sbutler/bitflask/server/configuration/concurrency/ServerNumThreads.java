package dev.sbutler.bitflask.server.configuration.concurrency;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@interface ServerNumThreads {

}

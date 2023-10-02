package dev.sbutler.bitflask.storage.raft;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Guice binding annotation for the {@link
 * com.google.common.util.concurrent.ListeningScheduledExecutorService} used by the {@link
 * RaftModeManager}.
 */
@Qualifier
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
@interface RaftModeManagerListeningExecutorService {}

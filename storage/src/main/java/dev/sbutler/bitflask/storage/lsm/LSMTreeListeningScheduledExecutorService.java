package dev.sbutler.bitflask.storage.lsm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Guice binding annotation for the {@link
 * com.google.common.util.concurrent.ListeningScheduledExecutorService} used by an {@link LSMTree}.
 */
@Qualifier
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
@interface LSMTreeListeningScheduledExecutorService {}

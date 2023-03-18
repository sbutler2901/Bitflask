package dev.sbutler.bitflask.storage.lsm;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Qualifier;

/**
 * Guice binding annotation for the
 * {@link com.google.common.util.concurrent.ListeningScheduledExecutorService} used by an
 * {@link LSMTree}.
 */
@Qualifier
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface LSMTreeListeningScheduledExecutorService {

}

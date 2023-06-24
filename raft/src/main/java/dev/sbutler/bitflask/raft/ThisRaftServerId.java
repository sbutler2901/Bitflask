package dev.sbutler.bitflask.raft;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** The {@link dev.sbutler.bitflask.raft.RaftServerId} of this server instance. */
@Qualifier
@Target({FIELD, PARAMETER, METHOD})
@Retention(RUNTIME)
public @interface ThisRaftServerId {}
